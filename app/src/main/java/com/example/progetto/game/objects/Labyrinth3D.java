package com.example.progetto.game.objects;

import android.graphics.Point;
import android.opengl.GLES30;

import androidx.annotation.NonNull;

import com.example.progetto.game.LabyrinthGenerator;

import java.util.Map;

import com.example.progetto.ogles.Geometry3D;
import com.example.progetto.ogles.Object3D;
import com.example.progetto.ogles.camera.CameraBase;
import com.example.progetto.ogles.shader.MaterialBasic;
import com.example.progetto.ogles.shader.ShaderProgram;

/**
 * Classe per la creazione del labirinto nello spazio 3D.
 */
public class Labyrinth3D {

    private final Object3D[] objLabyrinthWalls;
    private final Object3D objRoof;
    private final Object3D objFloor;

    /**
     * Costruttore della classe.
     *
     * Genera il labirinto nello spazio 3D.
     *
     * Il centro del labirinto coincide con il centro dello spazio 3D.
     * I muri del labirinto sono rappresentati da cubi unitari (1x1x1), mentre il tetto e il
     * pavimento sono definiti da piani di grandezza pari alla grandezza del labirinto.
     *
     * @param labGenerator LabGenerator
     * @param geometries HashMap di geometrie
     * @param materials HashMap di materiali
     */
    public Labyrinth3D(LabyrinthGenerator labGenerator,
                       Map<String, Geometry3D> geometries,
                       Map<String, MaterialBasic> materials){

        Point dim = labGenerator.getDimension();

        // creazione cubi del labirinto
        float[][] labWalls = labGenerator.getWallsCoord();
        objLabyrinthWalls = new Object3D[labWalls.length];
        MaterialBasic wallMat = materials.get("wall");
        assert wallMat != null;
        wallMat.setTextureScaling(1, -1);  // flip immagine
        for (int i=0 ; i<labWalls.length ; i++){
            Object3D obj = new Object3D(geometries.get("cube"), materials.get("wall")); // stessa geometry e material per tutti
            obj.setPosition(labWalls[i][0], 0, labWalls[i][1]);
            obj.setScale(0.5f, 0.5f, 0.5f); // cubi diventano di grandezza 1
            obj.updateModelM();
            objLabyrinthWalls[i] = obj;
        }

        // creazione floor
        MaterialBasic floorMat = materials.get("floor");
        assert floorMat != null;
        floorMat.setTextureScaling(dim.x*3, dim.y*3); // *3 per avere 3 piastrelle
        objFloor = new Object3D(geometries.get("plane"), floorMat);
        objFloor.setPosition(0, -0.5f, 0);
        objFloor.setScale(dim.x, 1, dim.y);
        objFloor.updateModelM();

        // creazione roof
        MaterialBasic roofMat = materials.get("roof");
        assert roofMat != null;
        roofMat.setTextureScaling(dim.x, dim.y);
        objRoof = new Object3D(geometries.get("plane"), roofMat);
        objRoof.setPosition(0, 0.5f, 0);
        objRoof.setRotation(180, 'x');  // così non ho problemi con il culling
        objRoof.setScale(dim.x, 1, dim.y);
        objRoof.updateModelM();

    }

    /**
     * Drawcalls per disegnare le pareti del labirnito.
     *
     * @param camera Camera prospettica
     */
    public void drawLabyrinthWalls(CameraBase camera){

        // bindo il VAO del primo elemento in quanto tutti condividono la stessa Geometry3D
        GLES30.glBindVertexArray(objLabyrinthWalls[0].getGeometry().getVao()[0]);

            objLabyrinthWalls[0].getMaterial().updateUniforms();   // aggiorno la uniform solo 1 volta per tutti
            objLabyrinthWalls[0].getMaterial().activateTexture();  // attivo solo la texture del primo (tutte uguali)
            for ( Object3D obj : objLabyrinthWalls ){
                obj.draw(camera);
            }

        GLES30.glBindVertexArray(0);

    }

    /**
     * Drawcalls per disegnare il pavimento e il tetto del labirinto.
     *
     * @param camera Camera prospettica
     */
    public void drawRoofAndFloor(CameraBase camera){

        // il VAO bindato è del plane geometry

        // roof
        objRoof.getMaterial().updateUniforms();
        objRoof.getMaterial().activateTexture();
        objRoof.draw(camera);

        // floor
        objFloor.getMaterial().updateUniforms();
        objFloor.getMaterial().activateTexture();
        objFloor.draw(camera);

    }

    @NonNull
    @Override
    public String toString(){

        StringBuilder res = new StringBuilder("Debug \nLabyrinth: \nobjLabyrinthWalls:\n");

        for (int i=0; i<objLabyrinthWalls.length; i++){

            res.append("\tobj").append(i).append(": VAO=").append(objLabyrinthWalls[i].getGeometry().getVao()[0]).append("; ShaderProgID: ").append(objLabyrinthWalls[i].getMaterial().getProgramId()).append("; TextureObjectID: ").append(objLabyrinthWalls[i].getMaterial().getTextureID()).append("\n");

        }

        res.append("objRoof: VAO=").append(objRoof.getGeometry().getVao()[0]).append("; ShaderProgID: ").append(objRoof.getMaterial().getProgramId()).append("; TextureObjectID: ").append(objRoof.getMaterial().getTextureID()).append("\n");

        res.append("objFloor: VAO=").append(objFloor.getGeometry().getVao()[0]).append("; ShaderProgID: ").append(objFloor.getMaterial().getProgramId()).append("; TextureObjectID: ").append(objFloor.getMaterial().getTextureID()).append("\n");


        return res.toString();
    }

    /**
     * Ritorna shaderProgram di un qualsiasi oggetto (tutti condividono lo stesso shaderProgram).
     *
     * @return shaderProgram condiviso da tutti
     */
    public ShaderProgram getCommonShaderProgram() { return objLabyrinthWalls[0].getMaterial().getShaderProgram(); }

    /**
     * Ritorna la Geometry3D del piano condiviso nella scena.
     *
     * @return Geometry3D del piano condiviso nella scena
     */
    public Geometry3D getCommonPlaneGeometry() { return objFloor.getGeometry(); }

}
