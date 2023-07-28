package com.example.progetto.ogles.camera;

import android.opengl.Matrix;

/**
 * Camera ortogonale che estende la classe base astratta.
 */
public class CameraOrtho2D extends CameraBase{

    /**
     * Costruttore classe che imposta la camera al centro dello spazio e con y=2.
     */
    public CameraOrtho2D(){

        super(0, 2, 0);

    }

    /**
     * Funzione per il setup della matrice di proiezione (ortogonale).
     *
     * Imposto la booleana a True per aggiornre le matrici quando necessario.
     *
     * @param left Estremo sinistra
     * @param right Estremo destra
     * @param bottom Estremo basso
     * @param top Estremo alto
     */
    public void setupProjection(float left, float right, float bottom, float top) {

        setupProjection(0, left, right, bottom, top);

    }

    /**
     * Funzione per il setup della matrice di proiezione (ortogonale).
     *
     * Imposto la booleana a True per aggiornre le matrici quando necessario.
     *
     * @param aspect aspect ratio (Non usato)
     * @param left Estremo sinistra
     * @param right Estremo destra
     * @param bottom Estremo basso
     * @param top Estremo alto
     */
    @Override
    public void setupProjection(float aspect, float left, float right, float bottom, float top) {

        Matrix.orthoM(projM,0,left, right, bottom, top,0.1f,100f);

        matrixNeedsUpdate = true;

    }
    /**
     * Funzione per aggiornare la matrice view e calcolare la PV.
     *
     * Imposto la booleana di aggiornamento delle matrici a False.
     *
     * Imposto upZ = -1 perchè la camera sta guardando lungo una direzione esattamente verticale e
     * quindi impostare upY = 1 porta ad avere i due vettori esattamente paralleli e questo è
     * problematico :
     *      https://stackoverflow.com/questions/26524013/opengl-is-not-displaying-anything-when-looking-along-the-y-axis
     *
     */
    @Override
    public void updateViewAndPvM() {

        // update viewM
        Matrix.setLookAtM(viewM, 0, position[0], position[1], position[2],
                         0, 0, 0,
                         0, 0, -1);

        // update pvM
        Matrix.multiplyMM(pvM, 0, projM, 0, viewM, 0);

    }

}
