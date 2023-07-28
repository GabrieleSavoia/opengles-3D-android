package com.example.progetto.ogles;

import static android.opengl.GLES20.GL_ARRAY_BUFFER;
import static android.opengl.GLES20.GL_ELEMENT_ARRAY_BUFFER;
import static android.opengl.GLES20.GL_FLOAT;
import static android.opengl.GLES20.GL_STATIC_DRAW;
import static android.opengl.GLES20.glBindBuffer;
import static android.opengl.GLES20.glBufferData;
import static android.opengl.GLES20.glEnableVertexAttribArray;
import static android.opengl.GLES20.glGenBuffers;
import static android.opengl.GLES20.glVertexAttribPointer;

import android.content.Context;
import android.opengl.GLES30;

import com.example.progetto.ogles.utils.PlyObject;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 * Classe per la gestione di una generica geometria 3D con vertici, uv ed indici.
 *
 * Tiene il riferimento diretto al VAO creato e al numero di indici della geometria.
 *
 * Dal momento che in questo progetto sono presenti 3 diverse geometrie (piano, cubo, triangolo)
 * e CIASCUNA di queste è USATA DA PIU' Object3D, allora la classe di seguito implementata torna utile
 * perchè mi permette di avere il riferimento diretto al VAO e al numero di indici di ogni geometria.
 */
public class Geometry3D {

    private final int[] vao;
    private int numIndices;

    private FloatBuffer vertexBuffer;
    private IntBuffer indexBuffer;

    /**
     * Costruttore della classe.
     *
     * Dopo aver fatto l'allocazione dei buffer host-side, genera il VAO ed esegue i trasferimenti
     * dei dati necessari.
     *
     * @param vertices Vertex attributes con position e uv: [ 'x', 'y', 'z', 'u', 'v', ... ]
     * @param indices Vettore di indici
     */
    public Geometry3D(float[] vertices, int[] indices){

        vao = new int[1];
        numIndices = 0;

        allocateBuffers(vertices, indices);
        glPrepare();

    }

    /**
     * Funzione per il caricamento di un file PLY.
     *
     * @param context Contesto
     * @param fileName Nome afile da caricare
     * @return Oggetto PLY
     */
    public static PlyObject loadPlyObject(Context context, String fileName){
        InputStream is;
        PlyObject po = null;

        try {
            is = context.getAssets().open(fileName);
            po = new PlyObject(is);
            po.parse();

        }catch(IOException | NumberFormatException e){
            e.printStackTrace();
            System.exit(-1);
        }

        return po;
    }

    /**
     * Allocazione pinned host-size veloce dei buffer necessari.
     *
     * ByteBuffer/FloatBuffer: usati per memorizzare buffer di grandi dimensioni host-side
     *            Buffer DIRETTO: ottimizzato, il suo contenuto può non far parte del normale
     *            heap garbage collected.
     *            Avviene l'allocazione esplicita ma la deallocazione avviene dalla JVM (implicita).
     *
     * @param vertices Vertex attributes con position e uv: [ 'x', 'y', 'z', 'u', 'v', ... ]
     * @param indices Vettore di indici
     */
    private void allocateBuffers(float[] vertices, int[] indices){

        vertexBuffer = ByteBuffer.allocateDirect(vertices.length * Float.BYTES)
                                 .order(ByteOrder.nativeOrder())
                                 .asFloatBuffer();
        vertexBuffer.put(vertices);
        vertexBuffer.position( 0);

        numIndices = indices.length;

        indexBuffer = ByteBuffer.allocateDirect(indices.length * Integer.BYTES)
                                .order(ByteOrder.nativeOrder())
                                .asIntBuffer();
        indexBuffer.put(indices);
        indexBuffer.position( 0);

    }

    /**
     * Funzione che crea VAO/VBO, effettua trasferimento dei buffer (nel device) e crea puntatori
     * con gli attribute locations.
     *
     * Devide memory: VRAM della GPU (dispositivi discreti) oppure memoria in RAM del sistema ma
     *                visibile solo alla GPU per dispositivi integrati.
     *
     * RISORSE: https://www.khronos.org/opengl/wiki/Tutorial2:_VAOs,_VBOs,_Vertex_and_Fragment_Shaders_(C_/_SDL)
     * VBO: OGGETTO opengl che rappresenta un handle ad un certo dato (es. pos. vertici, uv, ...)
     *      che contiene le informazioni dei vertici.
     *      Nella var. VBO ho una lista di interi che fanno da handle a questi buffer.
     *      Un VBO (identificato da un singolo intero) può essere creato, bindato e poi eliminato.
     *          - glGenBuffers(n, VBO, start_pos): crea 'n' buffer e salva i
     *                          relativi handle nel vettore 'VBO' partendo dalla 'start_pos'
     *          - glBindBuffer(target, VBO[i]): da qua in poi ogni chiamata gl si riferirà al
     *                          VBO con handle VBO[i]. Se passo 0 faccio unbind.
     *                          target=GL_ELEMENT_ARRAY_BUFFER per gli indici (così il sistema)
     *                          riesce a capire che sto passando gli indici.
     *          - glBufferData: alloco e trasferisco nella memoria del device
     *          - glVertexAttribPointer(attr_location, size, type, normalized, stride, offset):
     *                                          (descrive buffer layout)
     *                          attr_location: indica che il VBO bindato deve essere riferito
     *                          al vertex attribute (definito con 'in' nel vertex shader) avente
     *                          index 'attr_location'. Questo indice lo metto hardcodato oppure uso
     *                          un metodo che mi ritorna l'indice dato il nome della variabile
     *                          nello shader.
     *                          Con stride ed offset è possibile mettere nello stesso buffer es.
     *                          vertex pos, uv, normali, ecc.
     *          - glEnableVertexAttribArray(attr_location): abilita l'indice 'attr_location' per
     *                          poter essere usato
     *
     * VAO: (OpenGLES 3.0) OGGETTO opengl che rappresenta un insieme di VBO.
     *      Anche questo può essere creato, bindato ed eliminato.
     *      Si ricorda il buffer layout (nelle versioni vecchie era da esplicitare ogni volta nel
     *      onDrawFrame() ).
     */
    private void glPrepare(){

        int locvPos = 1;
        int locvUv = 2;

        GLES30.glGenVertexArrays(1, vao, 0);

        int [] VBO = new int[2];                   //0: vPos/vUv, 1: indices
        glGenBuffers(2, VBO, 0);

        GLES30.glBindVertexArray(vao[0]);
            // vPos, vUv
            glBindBuffer(GL_ARRAY_BUFFER, VBO[0]);
                glBufferData(GL_ARRAY_BUFFER, Float.BYTES * vertexBuffer.capacity(), vertexBuffer, GL_STATIC_DRAW);
                glVertexAttribPointer(locvPos, 3, GL_FLOAT, false, 5 * Float.BYTES, 0); //vPos
                glVertexAttribPointer(locvUv, 2, GL_FLOAT, false, 5 * Float.BYTES, 3 * Float.BYTES); //vUv
                glEnableVertexAttribArray(locvPos);
                glEnableVertexAttribArray(locvUv);
            // indices
            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, VBO[1]);
                glBufferData(GL_ELEMENT_ARRAY_BUFFER, Integer.BYTES * indexBuffer.capacity(), indexBuffer, GL_STATIC_DRAW);
            glBindBuffer(GL_ARRAY_BUFFER,0);
        GLES30.glBindVertexArray(0);

    }

    /******* GETTER *********/

    public int[] getVao(){ return vao; }  // handle

    public int getNumIndices(){
        return numIndices;
    }

}
