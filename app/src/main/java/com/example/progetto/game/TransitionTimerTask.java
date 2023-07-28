package com.example.progetto.game;

import android.util.Log;

import java.util.TimerTask;

import com.example.progetto.ogles.camera.CameraPersp3D;

/**
 * Classe che estende TimerTask con l'obiettivo di gestire le transizioni (rotazioni o traslazioni)
 * della camera.
 */
public class TransitionTimerTask extends TimerTask {

    private static String TAG;

    public static final int TRANSLATE_FW = 0;  // traslo avanti
    public static final int TRANSLATE_BW = 1;  // traslo indietro
    public static final int ROTATE_DX = 2;     // ruoto destra
    public static final int ROTATE_SX = 3;     // ruoto sinistra

    private final CameraPersp3D camera;
    private int transitionType;
    private float step;
    private float[] current;
    private float[] target;

    private boolean transitioning;
    private boolean awake;
    private final Object lock;

    /**
     * Costruttore della classe.
     *
     * @param camera Camera 3d prospettica di cui gestire la transizione
     */
    public TransitionTimerTask(CameraPersp3D camera){

        TAG = getClass().getSimpleName();

        this.camera = camera;
        transitionType = -1;
        step = 0f;
        current = new float[3];
        target = new float[3];

        transitioning = false;
        awake = true;
        lock = new Object();

    }

    /**
     * Funzione chiamata per far partire una certa transizione della camera (fatta solo 1 volta e
     * non per tutta la transizione).
     *
     * Il concetto è che dato un punto corrente, la camera viene ruotata o traslata di un certo
     * step fino a che non è raggiunto un punto target.
     *
     * @param transitionType Tipo di transizione (vedi costanti statiche della classe)
     * @param step Step di incremento ad ogni passo
     */
    public void startTransition (int transitionType, float step){

        this.transitionType = transitionType;

        switch (transitionType){
            case TRANSLATE_FW:
                this.step = step;
                current = camera.getPosition();
                target = camera.getPosOnLookAtDirection(1);
                break;
            case TRANSLATE_BW:
                this.step = -step;
                current = camera.getPosition();
                target = camera.getPosOnLookAtDirection(-1);
                break;
            case ROTATE_SX:
                this.step = step;
                current = new float[] { 0, camera.getRotationY(), 0 };
                target = new float[] { 0, camera.getRotationWithOffset(90), 0 };
                break;
            case ROTATE_DX:
                this.step = -step;
                current = new float[] { 0, camera.getRotationY(), 0 };
                target = new float[] { 0, camera.getRotationWithOffset(-90), 0 };
                break;
            default:
                Log.d(TAG, "Transizione non valida!");
                transitioning = false;
                return;
        }

        transitioning = true;

    }

    /**
     * Definisce il lavoro del thread periodico.
     *
     * Se è awake e transitioning allora incrementa traslazione o rotazione di un certo step e
     * controlla se il target è raggiunto.
     *
     * Se awake è false invece, effetto un wait bloccando così il thread fino a che non è fatto
     * un notify.
     */
    @Override
    public void run() {

        if (awake){

            if (transitioning){

                if ( (transitionType == TRANSLATE_FW) || (transitionType == TRANSLATE_BW) ){

                    current = camera.setPosition(camera.getPosOnLookAtDirection(step));

                }else if ( (transitionType == ROTATE_DX) || (transitionType == ROTATE_SX) ){

                    camera.setRotationY(camera.getRotationY() + step);
                    current = new float[] { 0, camera.getRotationY(), 0 };

                }

                checkTransition( current, target );

            }

        }else{

            Log.d(TAG,"Sleep");
            synchronized(lock) {
                try {
                    lock.wait();                   // rilascia il lock e aspetta che venga chiamato
                                                   // il notify()
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            Log.d(TAG,"Awake");

        }

    }

    /**
     * Funzione che controlla se il target è stato raggiunto.
     *
     * Nel caso in cui il target è raggiunto allora imposto come posizione o rotazione esattamente
     * il valore del target (per evitare eventuali imprecisioni).
     *
     * @param current Valore corrente
     * @param target Valore target da raggiungere
     */
    public void checkTransition(float[] current, float[] target){

        if ( (current == null) || (current.length != 3) ){ return; }
        if ( (target == null) || (target.length != 3) ){ return; }

        float val = Math.abs(step);

        if ( (Math.abs(current[0] - target[0]) < val) &&
             (Math.abs(current[1] - target[1]) < val) &&
             (Math.abs(current[2] - target[2]) < val) ){

            // End transition

            if ( (transitionType == TRANSLATE_FW) || (transitionType == TRANSLATE_BW) ){

                camera.setPosition(target);

            }else if ( (transitionType == ROTATE_DX) || (transitionType == ROTATE_SX) ){

                camera.setRotationY(target[1]);

            }

            float[] p = camera.getPosition();
            float[] d = camera.getLookAtDirection();
            Log.d(TAG,"End Transition: \nPosition: x: "+ p[0] + ", y: "+ p[1] + ", z: "+ p[2] +
            "\nRotation y: "+ camera.getRotationY()+ "\nLookAtDirection: "+ d[0]+ ", "+ d[1]+", "+ d[2] );

            transitioning = false;

        }

    }

    /**
     * Funzione che mi dice se una transizione è in atto.
     *
     * @return True se una transizione è in atto e deve ancora terminare, False se non ci sono
     *          transizioni in esecuzione
     */
    public boolean isTransitioning(){

        return transitioning;

    }

    /**
     * Funzione che mette in sleep il thread.
     */
    public void sleep(){

        awake = false;

    }

    /**
     * Funzione che risveglia il thread messo in sleep.
     */
    public void wakeup(){

        awake = true;

        synchronized(lock){
            lock.notify();
        }

    }
}