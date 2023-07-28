package com.example.progetto.game;

import static android.opengl.GLES20.GL_BACK;
import static android.opengl.GLES20.GL_CCW;
import static android.opengl.GLES20.GL_COLOR_BUFFER_BIT;
import static android.opengl.GLES20.GL_CULL_FACE;
import static android.opengl.GLES20.GL_DEPTH_BUFFER_BIT;
import static android.opengl.GLES20.GL_DEPTH_TEST;
import static android.opengl.GLES20.GL_LEQUAL;
import static android.opengl.GLES20.GL_SCISSOR_TEST;
import static android.opengl.GLES20.glClear;
import static android.opengl.GLES20.glClearColor;
import static android.opengl.GLES20.glCullFace;
import static android.opengl.GLES20.glDepthFunc;
import static android.opengl.GLES20.glEnable;
import static android.opengl.GLES20.glFrontFace;
import static android.opengl.GLES20.glScissor;
import static android.opengl.GLES20.glUseProgram;
import static android.opengl.GLES20.glViewport;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Point;
import android.opengl.GLES30;
import android.opengl.GLSurfaceView;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import com.example.progetto.game.objects.Labyrinth3D;
import com.example.progetto.game.objects.Map2D;
import com.example.progetto.ogles.camera.CameraPersp3D;

/**
 * Classe Renderer del LabyrinthGame.
 */
public class GameRenderer implements GLSurfaceView.Renderer {

    private static String TAG;

    private Context context;
    private GLSurfaceView surface;
    private final Point currentScreen;

    private final LabyrinthGame game;
    private final CameraPersp3D camera;

    private Labyrinth3D labyrinth3D;
    private Map2D map2D;

    /**
     * Costruttore della classe.
     *
     * @param game Riferimento a LabyrinthGame
     */
    public GameRenderer(LabyrinthGame game) {

        TAG = getClass().getSimpleName();

        currentScreen = new Point(0, 0);

        this.game = game;
        camera = this.game.getCamera();

        labyrinth3D = null;    // generato nella onCreated()
        map2D = null;          // generato nella onCreated()

    }

    /**
     * Set del context e della surface per il renderer attuale.
     *
     * Definisce l'evento del touch sulla surface.
     *
     * Il codice nel listener esegue nel main/ui thread.
     * https://developer.android.com/guide/components/processes-and-threads
     *
     * @param context context
     * @param surface surface
     */
    @SuppressLint("ClickableViewAccessibility")
    public void setContextAndSurface(Context context, GLSurfaceView surface) {

        this.context = context;
        this.surface = surface;

        // implementazione dell'interfaccia
        this.surface.setOnTouchListener(new View.OnTouchListener() {
            float previousX = 0;
            float previousY = 0;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event != null) {
                    switch (event.getAction() & MotionEvent.ACTION_MASK) {
                        case MotionEvent.ACTION_DOWN:
                            previousX = event.getX();
                            previousY = event.getY();
                            break;
                        case MotionEvent.ACTION_UP:
                            float newX = event.getX();
                            float newY = event.getY();
                            if (Math.abs(previousX - newX) > 50) {
                                if (previousX < newX) {
                                    game.rotate(TransitionTimerTask.ROTATE_SX);
                                } else {
                                    game.rotate(TransitionTimerTask.ROTATE_DX);
                                }
                            } else if (Math.abs(previousY - newY) > 50) {
                                if (previousY < newY) {
                                    game.translate(TransitionTimerTask.TRANSLATE_FW);
                                } else {
                                    game.translate(TransitionTimerTask.TRANSLATE_BW);
                                }
                            }
                            break;
                    }
                    return true;
                }
                return false;
            }
        });

    }

    /**
     * Chiamata quando la surface è creata o ricreata.
     * Una surface è ricreata nel momento in cui il suo contesto EGL è perso.
     * In questa funzione sono implementate le operazioni costose (allocazioni memoria,
     * trasferimenti, letture oggetti 3D/texture, compilazioni shader).
     *
     * Sono inoltre attivati i test necessari.
     *
     * @param gl10      gl (usato per compatibilità)
     * @param eglConfig config (usato per compatibilità)
     */
    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {

        Log.d(TAG, "onSurfaceCreated " + Thread.currentThread().getName());

        game.generate();
        labyrinth3D = game.getLabyrinth3D();
        map2D = game.getMap2D();

        //Log.d(TAG, labyrinth3D.toString() + map2D.toString());

        // 2 modi per disegnare la mappa tramite SCISSOR:
        //   1) attivo e disattivo SCISSOR nel onDrawFrame quando serve
        //   2) tengo SCISSOR sempre attivo e ridimensiono la sua dimensione all'occorrenza
        // Ho scelto la 2) e quindi all'inizio definisco le dimensioni dello SCISSOR pari a quelle
        // dello schermo poi quando è ora di disegnare la mappa lo ridimensiono.
        glEnable(GL_SCISSOR_TEST);   // Per-Sample operation

        glEnable(GL_DEPTH_TEST);     // Per-Sample operation (dopo il fragment shader)
        glDepthFunc(GL_LEQUAL);      // se frag. passa il depth-test, deth scritto nel depth buffer.
                                     // se frag. non passa depth-test, scarto fragment
                                     // GL_LEQUAL: frag. in uscita dal frag. shader passa il test se
                                     //            il suo depth value e <= di quello del depth buff
                                     //            nella stessa posizione.
                                     // Depth buffer serve quindi a salvarsi il valore di depth per
                                     // capire se un fragment passa il depth-test oppure no

        glEnable(GL_CULL_FACE);      // avviene nello stage primitive-assembly
        glCullFace(GL_BACK);
        glFrontFace(GL_CCW);         // antiorario

    }

    /**
     * Chiamata dopo onSurfaceCreated e ogni volta che avviene un cambio di dimensioni
     * della surface.
     *
     * @param gl10 gl (usato per compatibilità)
     * @param w    larghezza
     * @param h    altezza
     */
    @Override
    public void onSurfaceChanged(GL10 gl10, int w, int h) {

        Log.d(TAG, "onSurfaceChanged " + Thread.currentThread().getName());

        float aspect = ((float) w) / ((float) (h == 0 ? 1 : h));
        game.onSurfaceChanged(aspect, w, h);

        glViewport(0, 0, w, h);
        currentScreen.x = w;
        currentScreen.y = h;

    }

    /**
     * Chiamata ad ogni frame.
     * Si occupa di fare dispatch delle drawcall.
     *
     * Di default è bindato il default framebuffer (costituito dal color, depth e stancil buffer).
     *      Qualsiasi chiamata (glColor, ...) si riferisce al default frame buffer se non
     *      diversamente specificato.
     *
     * glClear è una early-operation (va diretta alla fine della pipeline).
     *         glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT):
     *             - colora il color buffer del colore settato da glClearColor
     *             - colora il depth buffer del colore settato da glClearDepthf
     *          Soggetta ad almeno 2 test:
     *          - Ownershipt test: (PER-SAMPLE) se ho un'altra finestra sopra la surface, i pixel
     *                             della surface sotto la finestra non passano il test (non li disegno)
     *                             in quanto non appartengono al contesto opengl.
     *          - Scissor test: (PER-SAMPLE) scarta (non disegna) pixel che sono al di fuori di una
     *                          certa regione definita da 'glScissor(x, y, width, height)'
     *
     * glViewport applicato nel vertex post-processing ed avviene PER-PRIMITIVA (es. triangolo).
     *            Definisce in che modo passare dalle coordinate NDC (range -1, 1 in entrambi gli
     *            assi) alla window-space.
     *            CLIPPING: se una parte di una primitiva si trova dentro al viewport ed una parte
     *                      fuori, allora avviene il CLIPPING, ovvero viene 'tagliata' la
     *                      primitiva (mantenendo solo la parte all'interno del viewport) e poi
     *                      ricostruita (in modo che ci siano solo triangoli)
     *
     * @param gl10 gl (usato per compatibilità)
     */
    @Override
    public void onDrawFrame(GL10 gl10) {

        glScissor(0, 0, currentScreen.x, currentScreen.y);
        glViewport(0, 0, currentScreen.x, currentScreen.y);
        glClearColor(0, 0.45f, 0.9f, 1f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        // Se necessario aggiorno la camera e poi la mappa 2D
        if (camera.matrixNeedsUpdate()){
            camera.updateViewAndPvM();
            map2D.updateFromCamera(camera);
        }

        // Program condiviso da tutti
        glUseProgram(labyrinth3D.getCommonShaderProgram().getProgramId());

            // LABIRINTO

            labyrinth3D.drawLabyrinthWalls(camera);

            // VAO = commonPlaneGeometry --> usato per tetto e paviemnto labirinto + mappa
            GLES30.glBindVertexArray(labyrinth3D.getCommonPlaneGeometry().getVao()[0]);
                labyrinth3D.drawRoofAndFloor(camera);

                // MAPPA

                map2D.drawMap2D(currentScreen);

            GLES30.glBindVertexArray(0);

                map2D.drawStartEndPointers();

        glUseProgram(0);

    }

    /****** GETTER *****/

    public Context getContext() { return context; }

    public GLSurfaceView getSurface() { return surface; }
}
