package com.example.progetto.ogles.camera;

import android.opengl.Matrix;

/**
 * Camera prospettica che estende la classe base astratta.
 *
 * JAVA MEMORY MODEL: https://jenkov.com/tutorials/java-concurrency/java-memory-model.html
 */
public class CameraPersp3D extends CameraBase{

    private final float[] lookAtDirection;
    private float rotationY;

    /**
     * Costruttore della classe prospettica.
     *
     * @param posX Posizione 'x' della camera
     * @param posY Posizione 'y' della camera
     * @param posZ Posizione 'z' della camera
     * @param rotationY rotazione sull'asse Y (in gradi)
     */
    public CameraPersp3D(float posX, float posY, float posZ, float rotationY){

        super(posX, posY, posZ);

        lookAtDirection = new float[3];
        setRotationY(rotationY);

    }

    /**
     * Funzione che setta la nuova posizione a partire da un vettore.
     *
     * Chiamare updateViewAndPvM() per aggiornare le matrici.
     *
     * @param newPosition Vettore ['x', 'y', 'z'] che rappresenta la nuova posizione
     */
    public synchronized float[] setPosition(float[] newPosition){

        if ( (newPosition==null) || (newPosition.length != 3) ){
            throw new IllegalArgumentException("Parametro non valido");
        }

        position[0] = newPosition[0];
        position[1] = newPosition[1];
        position[2] = newPosition[2];

        matrixNeedsUpdate = true;

        return position;

    }

    /**
     * Calcolo la posizione in world space del punto distante 'alpha' dalla camera
     * lungo la direzione di lookat.
     *
     * @param alpha Distanza dalla camera (>0 davanti la camera, <0 dietro la camera)
     * @return Posizione [x, y, z] in world space del punto distante 'alpha' dalla camera
     *         lungo la lookat
     */
    public synchronized float[] getPosOnLookAtDirection(float alpha){

        return new float[] { position[0]+lookAtDirection[0]*alpha,
                             position[1]+lookAtDirection[1]*alpha,
                             position[2]+lookAtDirection[2]*alpha };

    }

    /**
     * Funzione per la rotazione della camera.
     * I calcoli fatti sono per fare in modo che impostando 0° la camera guardi la z negativa e
     * che mettendo gradi positivi ruoti in senso antiorario.
     *
     * E' stato inoltre usato l'operatore modulo per fare in modo di non avere un valore di angolo
     * troppo elevato nel caso in cui accumulo le rotazioni.
     *
     * Chiamare updateViewAndPvM() per aggiornare le matrici.
     *
     * Faccio un arrotondamento perchè a volte il risultato di cos e sen non era preciso ma E-16.
     *
     * LINK: https://learnopengl.com/Getting-started/Camera
     *
     * @param rotationY Valore di rotazione (se positivo ruoto in senso antiorario). 0° guarda verso
     *                  z negativa.
     */
    public synchronized void setRotationY(float rotationY){

        this.rotationY = -( (rotationY % 360) + 90 );

        lookAtDirection[0] = (float) (Math.round( Math.cos( Math.toRadians(this.rotationY) ) * 1000.0 ) / 1000.0);
        lookAtDirection[1] = 0;
        lookAtDirection[2] = (float) (Math.round( Math.sin( Math.toRadians(this.rotationY) ) * 1000.0 ) / 1000.0);

        matrixNeedsUpdate = true;

    }

    /**
     * Funzione per il setup della matrice di proiezione (prospettica).
     *
     * Chiamare updateViewAndPvM() per aggiornare le matrici.
     *
     * @param aspect Aspect ratio
     */
    public void setupProjection(float aspect) {

        setupProjection(aspect, 0, 0, 0, 0);

    }

    /**
     * Funzione per il setup della matrice di proiezione (prospettica).
     *
     * Chiamare updateViewAndPvM() per aggiornare le matrici.
     *
     * Definisco un FOV    = 45°
     *              z-near = 0.1
     *              z-far  =100
     *
     * @param aspect Aspect ratio
     * @param left (Non utilizzato)
     * @param right (Non utilizzato)
     * @param bottom (Non utilizzato)
     * @param top (Non utilizzato)
     */
    @Override
    public void setupProjection(float aspect, float left, float right, float bottom, float top) {

        Matrix.perspectiveM(projM, 0, 45f, aspect, 0.1f, 100f);

        matrixNeedsUpdate = true;

    }

    /**
     * Funzione per aggiornare la matrice view e calcolare la PV.
     *
     * Imposto come lookat il valore 'position' + 'lookAtDirection' ovvero un punto lungo
     * la direzione di lookat.
     *
     */
    @Override
    public synchronized void updateViewAndPvM() {

        // update viewM
        Matrix.setLookAtM(viewM, 0, position[0], position[1], position[2],
                position[0]+lookAtDirection[0],
                position[1]+lookAtDirection[1],
                position[2]+lookAtDirection[2],
                0, 1, 0);

        // update pvM
        Matrix.multiplyMM(pvM, 0, projM, 0, viewM, 0);

        matrixNeedsUpdate = false;

    }

    /****** GETTER *******/

    public synchronized float getRotationY() { return -(rotationY+90); }

    @Override
    public synchronized float[] getPosition() { return super.getPosition(); }

    public synchronized float getRotationWithOffset(float offset){ return (getRotationY()+offset) % 360; }

    public float[] getLookAtDirection() { return lookAtDirection; }

}
