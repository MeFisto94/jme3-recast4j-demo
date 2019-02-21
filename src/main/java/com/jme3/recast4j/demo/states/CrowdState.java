/*
 * The MIT License
 *
 * Copyright 2019 .
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 * MODELS/DUNE.J3O:
 * Converted from http://quadropolis.us/node/2584 [Public Domain according to the Tags of this Map]
 */

package com.jme3.recast4j.demo.states;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.bullet.BulletAppState;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.recast4j.Detour.BetterDefaultQueryFilter;
import com.jme3.recast4j.Detour.Crowd.Crowd;
import com.jme3.recast4j.Detour.Crowd.Impl.CrowdManagerAppstate;
import com.jme3.recast4j.Detour.Crowd.MovementApplicationType;
import com.jme3.recast4j.Detour.DetourUtils;
import com.jme3.recast4j.Recast.GeometryProviderBuilder;
import com.jme3.recast4j.Recast.NavMeshDataCreateParamsBuilder;
import com.jme3.recast4j.Recast.RecastBuilderConfigBuilder;
import com.jme3.recast4j.Recast.RecastConfigBuilder;
import com.jme3.recast4j.Recast.RecastUtils;
import com.jme3.recast4j.Recast.SampleAreaModifications;
import com.jme3.recast4j.demo.controls.CrowdBCC;
import com.jme3.recast4j.demo.controls.CrowdDebugControl;
import com.jme3.recast4j.demo.controls.PhysicsAgentControl;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Torus;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteOrder;
import java.util.logging.Level;
import org.recast4j.detour.FindNearestPolyResult;
import org.recast4j.detour.MeshData;
import org.recast4j.detour.NavMesh;
import org.recast4j.detour.NavMeshBuilder;
import org.recast4j.detour.NavMeshDataCreateParams;
import org.recast4j.detour.NavMeshQuery;
import org.recast4j.detour.crowd.CrowdAgent;
import org.recast4j.detour.crowd.CrowdAgentParams;
import org.recast4j.detour.crowd.ObstacleAvoidanceQuery.ObstacleAvoidanceParams;
import org.recast4j.detour.io.MeshDataReader;
import org.recast4j.detour.io.MeshDataWriter;
import org.recast4j.detour.io.MeshSetReader;
import org.recast4j.detour.io.MeshSetWriter;
import org.recast4j.recast.PolyMesh;
import org.recast4j.recast.RecastBuilder;
import org.recast4j.recast.RecastBuilderConfig;
import org.recast4j.recast.RecastConfig;
import org.recast4j.recast.geom.InputGeomProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A procedural example of creating a crowd. When running this state and the gui
 * at the same time, they do not interfere with each other.
 * 
 * @author Robert
 */
public class CrowdState extends BaseAppState {

    private static final Logger LOG = LoggerFactory.getLogger(CrowdState.class.getName());
    
    private NavMeshQuery query;
    private Crowd crowd;
    
    @Override
    protected void initialize(Application app) {
        
        Box boxMesh = new Box(10f,.1f,10f); 
        Geometry boxGeo = new Geometry("Colored Box", boxMesh); 
        Material boxMat = new Material(getApplication().getAssetManager(), "Common/MatDefs/Light/Lighting.j3md"); 
        boxMat.setBoolean("UseMaterialColors", true); 
        boxMat.setColor("Ambient", ColorRGBA.Green); 
        boxMat.setColor("Diffuse", ColorRGBA.Green); 
        boxGeo.setMaterial(boxMat); 
        ((SimpleApplication) getApplication()).getRootNode().attachChild(boxGeo);
        
        //Step 1. Gather our geometry.
        InputGeomProvider geomProvider = new GeometryProviderBuilder(boxGeo).build();
        //Step 2. Create a Recast configuration object.
        RecastConfigBuilder builder = new RecastConfigBuilder();
        //Instantiate the configuration parameters.
        RecastConfig cfg = builder
                .withAgentRadius(0.4f)              // r
                .withAgentHeight(2.0f)              // h
                //cs and ch should be .1 at min.
                .withCellSize(0.2f)                 // cs=r/2
                .withCellHeight(0.1f)                // ch=cs/2 but not < .1f
                .withAgentMaxClimb(0.3f)             // > 2*ch
                .withAgentMaxSlope(45f)
                .withEdgeMaxLen(3.2f)               // r*8
                .withEdgeMaxError(1.3f)             // 1.1 - 1.5
                .withDetailSampleDistance(6.0f)     // increase if exception
                .withDetailSampleMaxError(5.0f)     // increase if exception
                .withVertsPerPoly(3).build();       
        //Create a RecastBuilderConfig builder with world bounds of our geometry.
        RecastBuilderConfigBuilder rcb = new RecastBuilderConfigBuilder(boxGeo);
        //Build the configuration object using our cfg. This is where we decide
        //if this is a solo NavMesh build or tiled. Tiled will be covered later.
        RecastBuilderConfig bcfg = rcb.build(cfg);        
        //Step 3. Build our Navmesh data using our gathered geometry and configuration.
        RecastBuilder rb = new RecastBuilder();
        RecastBuilder.RecastBuilderResult rbr = rb.build(geomProvider, bcfg);
        //Set the parameters needed to build our MeshData using the RecastBuilder results.
        NavMeshDataCreateParamsBuilder paramBuilder = new NavMeshDataCreateParamsBuilder(rbr);
        //Update poly flags from areas.
        PolyMesh pmesh = paramBuilder.getPolyMesh();
        for (int i = 0; i < pmesh.npolys; ++i) {
            if (pmesh.areas[i] == SampleAreaModifications.SAMPLE_POLYAREA_TYPE_GROUND
              || pmesh.areas[i] == SampleAreaModifications.SAMPLE_POLYAREA_TYPE_GRASS
              || pmesh.areas[i] == SampleAreaModifications.SAMPLE_POLYAREA_TYPE_ROAD) {
                paramBuilder.withPolyFlag(i, SampleAreaModifications.SAMPLE_POLYFLAGS_WALK);
            } else if (pmesh.areas[i] == SampleAreaModifications.SAMPLE_POLYAREA_TYPE_WATER) {
                paramBuilder.withPolyFlag(i, SampleAreaModifications.SAMPLE_POLYFLAGS_SWIM);
            } else if (pmesh.areas[i] == SampleAreaModifications.SAMPLE_POLYAREA_TYPE_DOOR) {
                paramBuilder.withPolyFlags(i, SampleAreaModifications.SAMPLE_POLYFLAGS_WALK
                | SampleAreaModifications.SAMPLE_POLYFLAGS_DOOR);
            }
            if (pmesh.areas[i] > 0) {
                pmesh.areas[i]--;
            }
        }
        //Build the parameter object. Set any flags here.
        NavMeshDataCreateParams params = paramBuilder.build(bcfg);
        //Step 4. Generate MeshData using our parameters object.
        MeshData meshData = NavMeshBuilder.createNavMeshData(params);
        //Step 5. Build the NavMesh.
        NavMesh navMesh = new NavMesh(meshData, bcfg.cfg.maxVertsPerPoly, 0);
        
        try {
            //Step 6. Save our work. Using compressed format.
            MeshDataWriter mdw = new MeshDataWriter();
            mdw.write(new FileOutputStream(new File("myMeshData.md")),  meshData, ByteOrder.BIG_ENDIAN, false);
            //Or the native format using tiles.
            MeshSetWriter msw = new MeshSetWriter();
            msw.write(new FileOutputStream(new File("myNavMesh.nm")), navMesh, ByteOrder.BIG_ENDIAN, false);
            
            //Read in saved MeshData and build new NavMesh.
            MeshDataReader mdr = new MeshDataReader();       
            MeshData savedMeshData = mdr.read(new FileInputStream("myMeshData.md"), 3);
            NavMesh navMeshFromData = new NavMesh(savedMeshData, 3, 0);
            showDebugMeshes(savedMeshData, true);
            //Or read in saved NavMesh.
            MeshSetReader msr = new MeshSetReader();
            NavMesh navMeshFromSaved = msr.read(new FileInputStream("myNavMesh.nm"), 3);
            
            //Create the query object for pathfinding in this Crowd. Will be 
            //added to the mapCrowds as a value so each crowd query object is
            //referenced.  
            query = new NavMeshQuery(navMeshFromSaved);
            //Start crowd.
            crowd = new Crowd(MovementApplicationType.DIRECT, 100, .3f, navMeshFromSaved);
            //Add to CrowdManager.
            getState(CrowdManagerAppstate.class).getCrowdManager().addCrowd(crowd);
            
            //Add OAP.
            ObstacleAvoidanceParams oap = new ObstacleAvoidanceParams();
            oap.velBias = 0.5f;
            oap.adaptiveDivs = 5;
            oap.adaptiveRings = 2;
            oap.adaptiveDepth = 1;
            crowd.setObstacleAvoidanceParams(0, oap);
            oap = new ObstacleAvoidanceParams();
            oap.velBias = 0.5f;
            oap.adaptiveDivs = 5;
            oap.adaptiveRings = 2;
            oap.adaptiveDepth = 2;
            crowd.setObstacleAvoidanceParams(1, oap);
            oap = new ObstacleAvoidanceParams();
            oap.velBias = 0.5f;
            oap.adaptiveDivs = 7;
            oap.adaptiveRings = 2;
            oap.adaptiveDepth = 3;
            crowd.setObstacleAvoidanceParams(2, oap);
            oap = new ObstacleAvoidanceParams();
            oap.velBias = 0.5f;
            oap.adaptiveDivs = 7;
            oap.adaptiveRings = 3;
            oap.adaptiveDepth = 3;
            
        } catch (IOException | NoSuchFieldException | IllegalAccessException ex) {
            LOG.info("{} {}", CrowdBuilderState.class.getName(), ex);
        }
    }

    @Override
    protected void cleanup(Application app) {
        
    }

    //onEnable()/onDisable() can be used for managing things that should 
    //only exist while the state is enabled. Prime examples would be scene 
    //graph attachment or input listener attachment.
    @Override
    protected void onEnable() {      
        addAgent(new Vector3f(-5, 0, 0));
        addAgent(new Vector3f(-4f, 0.0f, -1f));
        addAgent(new Vector3f(-3, 0, 0));         
    }

    @Override
    protected void onDisable() {
    }
    
    @Override
    public void update(float tpf) {

    }
    
    /**
     * Set the target for the selected crowd.
     * 
     * @param target The target to set.
     */
    public void setTarget(Vector3f target) {

        //Get the query extent for this crowd.
        float[] ext = crowd.getQueryExtents();

        //Locate the nearest poly ref/pos.
        FindNearestPolyResult nearest = query.findNearestPoly(DetourUtils.toFloatArray(target), ext, new BetterDefaultQueryFilter());

        if (nearest.getNearestRef() == 0) {
            LOG.info("getNearestRef() can't be 0. ref [{}]", nearest.getNearestRef());
        } else {
            //Sets all agent targets at same time.
            crowd.requestMoveToTarget(DetourUtils.createVector3f(nearest.getNearestPos()), nearest.getNearestRef());
        }
    }
    
    private void addAgent(Vector3f location) {
        
        //Load the spatial that will represent the agent.
        Node agent = (Node) getApplication().getAssetManager().loadModel("Models/Jaime/Jaime.j3o");
        //Set translation prior to adding controls.
        agent.setLocalTranslation(location);
        //We have a physics crowd so we need a physics compatible control to apply
        //movement and direction to the spatial.
        ((SimpleApplication) getApplication()).getRootNode().attachChild(agent);
        
        int updateFlags = CrowdAgentParams.DT_CROWD_OPTIMIZE_TOPO | CrowdAgentParams.DT_CROWD_OPTIMIZE_VIS;
        //Build the params object.
        CrowdAgentParams ap = new CrowdAgentParams();
        ap.radius                   = 0.03f;
        ap.height                   = 1.5f;
        ap.maxAcceleration          = 8.0f;
        ap.maxSpeed                 = 3.5f;
        ap.collisionQueryRange      = 12.0f;
        ap.pathOptimizationRange    = 30.0f;
        ap.separationWeight         = 2.0f;
        ap.updateFlags              = updateFlags;
        ap.obstacleAvoidanceType    = 0;
        
        //Were going to use a debug move control so setup geometry for later use.
        Torus halo = new Torus(16, 16, 0.1f, 0.3f);
        Geometry haloGeom = new Geometry("halo", halo);
        Material haloMat = new Material(getApplication().getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        haloGeom.setMaterial(haloMat);
        haloGeom.setLocalTranslation(0, ap.height + 0.5f, 0);
        Quaternion pitch90 = new Quaternion();
        pitch90.fromAngleAxis(FastMath.PI/2, new Vector3f(1,0,0));
        haloGeom.setLocalRotation(pitch90);

        //Add agent to the crowd.
        CrowdAgent createAgent = crowd.createAgent(agent.getWorldTranslation(), ap);
        //Set the spatial for the agent.
        crowd.setSpatialForAgent(createAgent, agent);        
        //Add the debug control and set its visual and verbose state.
        CrowdDebugControl dmc = new CrowdDebugControl(crowd, createAgent, haloGeom.clone());
        dmc.setVisual(true); 
        dmc.setVerbose(false);                    
        agent.addControl(dmc);
    }
    
        private void showDebugMeshes(MeshData meshData, boolean wireframe) {
        Material matRed = new Material(getApplication().getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        matRed.setColor("Color", ColorRGBA.Red);
        
        if (wireframe) {
            matRed.getAdditionalRenderState().setWireframe(true);
        }

        Material matGreen = new Material(getApplication().getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        matGreen.setColor("Color", ColorRGBA.Green);

        // navMesh.getTile(0).data == meshData (in this particular case)
        Geometry gDetailed = new Geometry("DebugMeshDetailed", RecastUtils.getDebugMesh(meshData.detailMeshes, meshData.detailVerts, meshData.detailTris));
        Geometry g = new Geometry("DebugMeshSimple", RecastUtils.getDebugMesh(meshData));
        g.setMaterial(matRed);
        gDetailed.setMaterial(matGreen);
        System.out.println("VertCount Regular Mesh: " + g.getVertexCount());
        System.out.println("VertCount Detailed Mesh: " + gDetailed.getVertexCount());
        g.move(0f, 0.125f, 0f);
        gDetailed.move(0f, 0.25f, 0f);

        ((SimpleApplication) getApplication()).getRootNode().attachChild(g);
        ((SimpleApplication) getApplication()).getRootNode().attachChild(gDetailed);
    }
}
