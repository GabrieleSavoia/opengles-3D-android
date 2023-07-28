package com.example.progetto.ogles.shader;

import static android.opengl.GLES20.glGetUniformLocation;

import java.util.HashMap;
import java.util.Map;

import com.example.progetto.ogles.utils.ShaderCompiler;

/**
 * Classe che si occupa di compilare Vertex / Fragment Shader e creare il relativo GL Program.
 * Tiene un riferimento diretto sia al program ID (handle) che ad una HashMap contenente le
 * locazioni delle uniforms (così calcolo 1 sola volta le locazioni e posso usare la hashmap per
 * leggerle).
 *
 * Compilazione:
 *      1) Per ogni shader (vertex, fragment):
 *              - creo GL Shader Object;
 *              - bindo shader src con il GL Shader Object creato;
 *              - compilo GL Shader Object
 *      2) Creo GL Program:
 *      3) Attach dei 2 GL Shader Object con GL Program;
 *      4) Link program e check error;
 *      5) Detach e delete degli shader.
 *
 * Utile perchè permette di avere tanti MaterialBasic (ognuno con i propri valori di uniform e texture
 * diverse) che puntano tutti allo stesso ShaderProgram ed inoltre mantiene le locazioni delle
 * uniform così le calcolo solo una volta.
 */
public class ShaderProgram {

    private int programId;   // (handle)
    private final Map<String, Integer> uniformLocMap;

    /**
     * Costruttore della classe.
     *
     * Viene creato il program in funzione ai vertex e fragment shader passati e poi sono calcolate
     * le locazioni delle uniforms.
     *
     * @param vs Vertex Shader
     * @param fs Fragment Shader
     * @param uniforms Lista di uniform di cui calcolare la locazione
     */
    public ShaderProgram(String vs, String fs, String[] uniforms){

        programId = -1;
        uniformLocMap = new HashMap<>();

        glPrepare(vs, fs);
        findUniformLocations(uniforms);

    }

    /**
     * Funzione che si occupa della compilazione e della creazione dello shader program.
     *
     * @param vs Vertex Shader
     * @param fs Fragment Shader
     */
    private void glPrepare(String vs, String fs){

        programId = ShaderCompiler.createProgram(vs, fs);

        if( programId < 0 ) {
            System.exit(-1);
        }

    }

    /**
     * Funzione che salva nella HashMap le locazioni delle uniforms.
     *
     * Chiamata una sola volta nel onSurfaceCreated() così che ogni volta non devo ricalcolare le
     * locazioni nel onDrawFrame().
     *
     * @param uniforms Lista dei nomi delle uniform di cui salvare la locazione
     */
    private void findUniformLocations(String[] uniforms){

        for (String uniform: uniforms){
            uniformLocMap.put(uniform, glGetUniformLocation(programId, uniform));
        }

    }

    /****** GETTER ******/

    public int getProgramId() { return programId; }

    public int getUniformLoc( String uniformName ){ return uniformLocMap.get(uniformName); }

}
