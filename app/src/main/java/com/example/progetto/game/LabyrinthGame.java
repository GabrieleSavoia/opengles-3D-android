package com.example.progetto.game;

import android.content.Context;
import android.graphics.Point;
import android.util.Log;
import android.widget.Toast;

import com.example.progetto.R;
import com.example.progetto.game.objects.Labyrinth3D;
import com.example.progetto.game.objects.Map2D;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;

import com.example.progetto.ogles.Geometry3D;
import com.example.progetto.ogles.Texture;
import com.example.progetto.ogles.camera.CameraPersp3D;
import com.example.progetto.ogles.shader.MaterialBasic;
import com.example.progetto.ogles.shader.ShaderProgram;
import com.example.progetto.ogles.utils.PlyObject;

/**
 * Classe per la gestione del gioco del labirinto.
 * La dimensione del labirinto è impostata come costante statica.
 */
public class LabyrinthGame {

    private static String TAG;
    private final Context context;

    private static final Point DIMENSION = new Point(15, 15);
    private final LabyrinthGenerator labGenerator;

    private final CameraPersp3D camera;
    private Labyrinth3D labyrinth3D;
    private Map2D map2D;

    private Timer timer;
    private TransitionTimerTask transitionTimerTask;

    /**
     * Costruttore della classe.
     *
     * E' fatto partire il timer per la gestione delle animazioni.
     *
     * @param context Activity context
     */
    public LabyrinthGame(Context context){

        TAG = getClass().getSimpleName();
        this.context = context;

        labGenerator = new LabyrinthGenerator(DIMENSION);

        camera = new CameraPersp3D(0.0f, 0.0f, 3.0f, 0);
        labyrinth3D = null;   // creato nella funzione "generate"
        map2D = null;         // creato nella funzione "generate"

        timer = new Timer();
        transitionTimerTask = new TransitionTimerTask(camera);
        timer.scheduleAtFixedRate(transitionTimerTask, 10, 5);

    }

    /**
     * Funzione chiamata nel onSurfaceChanged() del renderer.
     *
     * @param aspect Aspect ratio surface
     * @param w Larghezza surface
     * @param h Altezza surface
     */
    public void onSurfaceChanged(float aspect, int w, int h){

        camera.setupProjection(aspect);
        map2D.setupProjection(w, h);

    }

    /**
     * Funzione che genera un nuovo labirinto tramite un apposito algoritmo procedurale e poi crea
     * il realtivo spazio 3D con la mappa 2D.
     *
     * In questa funzione sono create le geometrie e i materiali che saranno poi usati per la
     * creazione del labirinto e della mappa.
     *
     * Imposta inoltre la posizione iniziale della camera in funzione ai risultati dell'algoritmo
     * di generazione del labirinto.
     */
    public void generate(){

        // Generazione procedurale labirinto
        labGenerator.generate();

        // Creazione nell'ambiente 3D (con mappa) del labirinto generato

        // Geometrie
        Map<String, Geometry3D> geometries = new HashMap<>();
        PlyObject po = Geometry3D.loadPlyObject(context, "cube.ply");
        geometries.put("cube", new Geometry3D(po.getVertices(), po.getIndices()));
        geometries.put("plane", new Geometry3D(new float[] {
                -0.5f, 0.0f, 0.5f, 0.0f, 0.0f,    // basso SX
                0.5f, 0.0f, 0.5f, 1.0f, 0.0f,     // basso DX
                0.5f, 0.0f, -0.5f, 1.0f, 1.0f,    // alto DX
                -0.5f, 0.0f, -0.5f, 0.0f, 1.0f,   // alto SX
        }, new int[] { 0, 1, 2,   0, 2, 3 }));
        geometries.put("triangle", new Geometry3D(new float[] {
                -0.45f, 0.0f, 0.45f, 0.0f, 0.0f,   // basso SX
                0.45f, 0.0f, 0.45f, 1.0f, 0.0f,    // basso DX
                0.0f, 0.0f, -0.5f, 1.0f, 1.0f,     // alto centrale
        }, new int[] { 0, 1, 2 }));

        // Materiali
        Map<String, MaterialBasic> materials = new HashMap<>();
        MaterialBasic mat = new MaterialBasic(new Texture(Texture.loadBitmap(context,
                                                           R.drawable.wall), true));
        ShaderProgram commonShaderProgram = mat.getShaderProgram();
        materials.put("wall", mat);
        materials.put("roof", new MaterialBasic(commonShaderProgram,
                                                   new Texture(Texture.loadBitmap(context,
                                                           R.drawable.roof), true)));
        materials.put("floor", new MaterialBasic(commonShaderProgram,
                                                   new Texture(Texture.loadBitmap(context,
                                                           R.drawable.floor), true)));
        materials.put("mapWall", new MaterialBasic(commonShaderProgram,
                                                   new Texture(Texture.loadBitmap(context,
                                                           R.drawable.mapwall), false)));
        materials.put("mapFloor", new MaterialBasic(commonShaderProgram,
                                                   new Texture(Texture.loadBitmap(context,
                                                           R.drawable.mapfloor), false)));
        materials.put("start", new MaterialBasic(commonShaderProgram, new float[]{1f, 0f, 0f} ));
        materials.put("end", new MaterialBasic(commonShaderProgram, new float[]{0f, 0f, 1f} ));

        labyrinth3D = new Labyrinth3D(labGenerator, geometries, materials);
        map2D = new Map2D(labGenerator, geometries, materials);

        setStartPosition();

    }

    /**
     * Funzione che aggiorna la posizione e rotazione iniziale della camera in funzione al risultato
     * dell'algoritmo di generazione del labirinto.
     */
    public void setStartPosition(){

        float[] startPos = labGenerator.getStartPoint();
        float startAngle = labGenerator.getStartAngle();

        camera.setPosition(startPos[0], 0, startPos[1]);
        camera.setRotationY(startAngle);

    }

    /**
     * Funzione che fa ruotare la camera verso destra o sinistra con una transizione gestita
     * dal TimerTask della classe.
     *
     * Nel caso in cui ci sia ancora una transizione in atto, la richiesta di rotazione
     * viene ignorata.
     *
     * @param transitionType tipo di transizione (vedi costanti statiche della classe
     *                       TransitionTimerTask)
     */
    public void rotate(int transitionType){

        if (transitionTimerTask.isTransitioning()){ return; }

        transitionTimerTask.startTransition(transitionType, 0.5f);

    }

    /**
     * Funzione che fa traslare la camera in avanti oppure indietro con una transizione gestita
     * dal TimerTask della classe.
     *
     * In questo caso è necessario controllare che la posizione target da raggiungere sia
     * effettivamente una posizione "walkable" e che quindi non si trovi al di fuori del labirinto
     * oppure che non rappresenti la posizione un muro.
     *
     * Se la posizione target corrisponde all'uscita del labirinto, allora mostro un Toast di avviso.
     *
     * Nel caso in cui ci sia ancora una transizione in atto, la richiesta di traslazione
     * viene ignorata.
     *
     * @param transitionType tipo di transizione (vedi costanti statiche della classe
     *                       TransitionTimerTask)
     */
    public void translate(int transitionType){

        if (transitionTimerTask.isTransitioning()){ return; }

        float[] targetPos = new float[3];
        if (transitionType == TransitionTimerTask.TRANSLATE_FW) {
            targetPos = camera.getPosOnLookAtDirection(1);
        }else if (transitionType == TransitionTimerTask.TRANSLATE_BW) {
            targetPos = camera.getPosOnLookAtDirection(-1);
        }

        if (!labGenerator.isWalkable(targetPos[0], targetPos[2])){
            Log.d(TAG, "Non walkable!!");
            return;
        }

        if (exitFound(targetPos)) {
            Toast.makeText(context,"Complimenti, hai trovato l'uscita!",
                                        Toast.LENGTH_LONG).show();
        }

        transitionTimerTask.startTransition(transitionType, 0.01f);

    }

    /**
     * Funzione che controlla se l'utente ha trovato l'uscita del labirinto.
     *
     * @return True se 'position'='endPoint', ovvero l'uscita del labirinto
     */
    public boolean exitFound(float[] position){

        float[] endPos = labGenerator.getEndPoint();

        return ( (position[0] == endPos[0]) &&
                (position[1] == 0) &&
                (position[2] == endPos[1]) );

    }

    /****** GETTER *******/

    public CameraPersp3D getCamera() { return camera; }

    public Labyrinth3D getLabyrinth3D() { return labyrinth3D; }

    public Map2D getMap2D() { return map2D; }

    public Timer getTimer() { return timer; }

    public TransitionTimerTask getTransitionTimerTask() { return transitionTimerTask; }

    /******* SETTER *******/

    public void setTimer(Timer timer) { this.timer = timer; }

    public void setTransitionTimerTask(TransitionTimerTask t) { transitionTimerTask = t; }

}
