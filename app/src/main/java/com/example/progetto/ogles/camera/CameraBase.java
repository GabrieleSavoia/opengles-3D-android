package com.example.progetto.ogles.camera;

import android.opengl.Matrix;

/**
 * Classe base astratta per la rappresentazione di una camera nello spazio 3D.
 */
public abstract class CameraBase {

    protected static String TAG;

    protected float[] viewM;
    protected float[] projM;
    protected float[] pvM;

    protected float[] position;

    protected boolean matrixNeedsUpdate;

    /**
     * Costruttore classe che prende in input la posizione.
     *
     * @param posX Posizione 'x' della camera
     * @param posY Posizione 'y' della camera
     * @param posZ Posizione 'z' della camera
     */
    public CameraBase(float posX, float posY, float posZ){

        TAG = getClass().getSimpleName();

        viewM = new float[16];
        projM = new float[16];
        pvM = new float[16];

        Matrix.setIdentityM(viewM, 0);
        Matrix.setIdentityM(projM, 0);
        Matrix.setIdentityM(pvM, 0);

        this.position = new float[3];
        setPosition(posX, posY, posZ);

    }

    /**
     * Setter per la posizione della camera.
     *
     * Imposto la booleana a True per aggiornre le matrici quando necessario.
     *
     * @param x Coordinata 'x' in world space
     * @param y Coordinata 'y' in world space
     * @param z Coordinata 'z' in world space
     */
    public void setPosition(float x, float y, float z){

        position[0] = x;
        position[1] = y;
        position[2] = z;

        matrixNeedsUpdate = true;

    }

    /**
     * Metodo astratto: ogni camera definirà la propria proiezione
     */
    public abstract void setupProjection(float aspect,
                                         float left, float right, float bottom, float top);

    /**
     * Metodo astratto: sarà implementato in maniera diversa per ogni tipo di camera.
     */
    public abstract void updateViewAndPvM();


    /******* GETTER *******/

    public float[] getPvM() { return pvM; }

    public float[] getPosition() { return position; }

    public boolean matrixNeedsUpdate()  { return matrixNeedsUpdate; }

}
