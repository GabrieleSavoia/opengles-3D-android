package com.example.progetto.ogles;

import static android.opengl.GLES20.GL_TRIANGLES;
import static android.opengl.GLES20.GL_UNSIGNED_INT;
import static android.opengl.GLES20.glDrawElements;

import android.opengl.Matrix;

import com.example.progetto.ogles.camera.CameraBase;
import com.example.progetto.ogles.shader.MaterialBasic;

/**
 * Classe per la gestione di posizione, rotazione e scala di un generico oggetto 3D.
 * Un Object3D è creato in funzione di una Geometry3D e di un MaterialBasic.
 *
 * E' possibile che più Object3D utilizzano una stessa risorsa (stesso Geometry3D o MaterialBasic).
 * Ad esempio se voglio creare 2 piani (stessi vertici) ma con dimensioni e posizione diverse,
 * creo 2 Object3D distinti a cui però passo lo stesso Geometry3D.
 *
 * Ogni Object3D ha salvato le proprie informazioni di posizione, rotazione e scaling.
 */
public class Object3D {

    protected static String TAG;

    private final Geometry3D geometry;
    private final MaterialBasic material;

    private final float[] position;
    private final int[] axesRotation;
    private float rotation;
    private final float[] scale;
    private final float[] modelM;

    private final float[] mvp;

    private boolean matrixNeedsUpdate;

    /**
     * Costruttore della classe.
     *
     * @param geometry Geometry3D contenente il riferimento al VAO
     * @param material MaterialBasic da utilizzare per l' Object3D
     */
    public Object3D(Geometry3D geometry, MaterialBasic material){

        TAG = getClass().getSimpleName();

        this.geometry = geometry;
        this.material = material;

        position = new float[] {0, 0, 0};
        axesRotation = new int[] {1, 1, 1};
        rotation = 0f;
        scale = new float[] {1, 1, 1};
        modelM = new float[16];

        mvp = new float[16];

        matrixNeedsUpdate = true;

    }

    /**
     * Setter della posizione dell'oggetto nello spazio 3D.
     *
     * Chiamare updateModelM() per aggiornare la model matrix.
     *
     * @param x Coordianta 'x'
     * @param y Coordianta 'y'
     * @param z Coordianta 'z'
     */
    public void setPosition(float x, float y, float z){

        position[0] = x;
        position[1] = y;
        position[2] = z;

        matrixNeedsUpdate = true;

    }

    /**
     * Setter della posizione dell'oggetto nello spazio 3D.
     *
     * Chiamare updateModelM() per aggiornare la model matrix.
     *
     * @param position Vettore ['x', 'y', 'z']
     */
    public void setPosition(float [] position){

        if ( (position==null) || (position.length != 3) ){
            throw new IllegalArgumentException("Parametro non valido");
        }

        this.position[0] = position[0];
        this.position[1] = position[1];
        this.position[2] = position[2];

        matrixNeedsUpdate = true;

    }

    /**
     * Setter della rotazione (in gradi) dell'oggetto su un certo asse d rotazione.
     *
     * Chiamare updateModelM() per aggiornare la model matrix.
     *
     * @param angle Angolo di rotazione in gradi. (angolo > 0 ruota in senso antiorario su asse 'y')
     * @param axes Asse di rotazione. Es: 'x', 'y', 'z'
     */
    public void setRotation(float angle, char axes){

        if ( (axes!='x') && (axes!='y') && (axes!='z') ){
            throw new IllegalArgumentException("Parametro non valido");
        }

        switch (axes){
            case 'x':
                axesRotation[0] = 1;
                axesRotation[1] = 0;
                axesRotation[2] = 0;
                break;
            case 'y':
                axesRotation[0] = 0;
                axesRotation[1] = 1;
                axesRotation[2] = 0;
                break;
            case 'z':
                axesRotation[0] = 0;
                axesRotation[1] = 0;
                axesRotation[2] = 1;
                break;
        }

        this.rotation = angle;

        matrixNeedsUpdate = true;

    }

    /**
     * Setter dello scale sui vari assi.
     *
     * Chiamare updateModelM() per aggiornare la model matrix.
     *
     * @param x Scaling su 'x'
     * @param y Scaling su 'y'
     * @param z Scaling su 'z'
     */
    public void setScale(float x, float y, float z){

        scale[0] = x;
        scale[1] = y;
        scale[2] = z;

        matrixNeedsUpdate = true;

    }

    /**
     * Aggiorno la metrice Model con i valori di postion, rotation e scale impostati.
     *
     * modelMatrix (4x4) * vertex_local_space (4x1) = vertex_world_space (4x1)
     *
     * Matrici salvate column-major. E' come se le applicassi al contrario.
     *
     */
    public void updateModelM(){

        Matrix.setIdentityM(modelM, 0);
        Matrix.translateM(modelM, 0, position[0], position[1], position[2]);
        Matrix.rotateM(modelM, 0, rotation, axesRotation[0], axesRotation[1], axesRotation[2]);
        Matrix.scaleM(modelM, 0, scale[0], scale[1], scale[2]);

        matrixNeedsUpdate = false;

    }

    /**
     * Funzione chiamata nel onDrawFrame() del renderer e disegna l' Object3D.
     *
     * @param camera Camera utilizzata per disegnare l'oggetto a video
     */
    public void draw(CameraBase camera){

        Matrix.multiplyMM(mvp, 0, camera.getPvM(), 0, modelM, 0);
        material.updateMVP(mvp);
        glDrawElements(GL_TRIANGLES, geometry.getNumIndices(), GL_UNSIGNED_INT,0);

    }

    /******* GETTER ********/

    public MaterialBasic getMaterial() { return material; }

    public Geometry3D getGeometry() { return geometry; }

    public boolean matrixNeedsUpdate() { return matrixNeedsUpdate; }

}
