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
import com.jme3.bullet.control.BetterCharacterControl;
import com.jme3.material.Material;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.recast4j.Detour.BetterDefaultQueryFilter;
import com.jme3.recast4j.Detour.Crowd.Crowd;
import com.jme3.recast4j.Detour.Crowd.Impl.CrowdManagerAppstate;
import com.jme3.recast4j.Detour.Crowd.MovementApplicationType;
import com.jme3.recast4j.Detour.DetourUtils;
import com.jme3.recast4j.demo.controls.CrowdBCC;
import com.jme3.recast4j.demo.controls.DebugMoveControl;
import com.jme3.recast4j.demo.controls.PhysicsAgentControl;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Torus;
import java.io.FileInputStream;
import java.io.IOException;
import org.recast4j.detour.FindNearestPolyResult;
import org.recast4j.detour.NavMesh;
import org.recast4j.detour.NavMeshQuery;
import org.recast4j.detour.crowd.CrowdAgent;
import org.recast4j.detour.crowd.CrowdAgentParams;
import org.recast4j.detour.crowd.ObstacleAvoidanceQuery.ObstacleAvoidanceParams;
import org.recast4j.detour.io.MeshSetReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Robert
 */
public class CrowdState extends BaseAppState {

    private static final Logger LOG = LoggerFactory.getLogger(CrowdState.class.getName());
    
    private NavMeshQuery query;
    private Crowd crowd;
    
    @Override
    protected void initialize(Application app) {
        MeshSetReader msr = new MeshSetReader();
        try {
            //Read in the saved navMesh with same maxVertPerPoly(3) saved. Will 
            //be added to mapCrowds as a key using the text returned by 
            //fieldCrowdName.
            NavMesh navMesh = msr.read(new FileInputStream("test.nm"), 3);
            //Create the query object for pathfinding in this Crowd. Will be 
            //added to the mapCrowds as a value so each crowd query object is
            //referenced.  
            query = new NavMeshQuery(navMesh);
            //Start crowd.
            crowd = new Crowd(MovementApplicationType.BETTER_CHARACTER_CONTROL, 100, .3f, navMesh);
            //Add to CrowdManager.
            getState(CrowdManagerAppstate.class).getCrowdManager().addCrowd(crowd);
            
            //Add OAP.
            ObstacleAvoidanceParams params = new ObstacleAvoidanceParams();
            params.velBias = 0.5f;
            params.adaptiveDivs = 5;
            params.adaptiveRings = 2;
            params.adaptiveDepth = 1;
            crowd.setObstacleAvoidanceParams(0, params);
            params = new ObstacleAvoidanceParams();
            params.velBias = 0.5f;
            params.adaptiveDivs = 5;
            params.adaptiveRings = 2;
            params.adaptiveDepth = 2;
            crowd.setObstacleAvoidanceParams(1, params);
            params = new ObstacleAvoidanceParams();
            params.velBias = 0.5f;
            params.adaptiveDivs = 7;
            params.adaptiveRings = 2;
            params.adaptiveDepth = 3;
            crowd.setObstacleAvoidanceParams(2, params);
            params = new ObstacleAvoidanceParams();
            params.velBias = 0.5f;
            params.adaptiveDivs = 7;
            params.adaptiveRings = 3;
            params.adaptiveDepth = 3;
            
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
        addAgent(new Vector3f(-10, 0, 0));
        addAgent(new Vector3f(-7.5f, 0.0f, -5f));
        addAgent(new Vector3f(-5, 0, 0));         
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
        agent.addControl(new CrowdBCC(0.3f, 1.5f, 20f)); // values taken from recast defaults
//        agent1.addControl(new BetterCharacterControl(0.3f, 1.5f, 20f)); // values taken from recast defaults
        //A control that checks our path list and advances our waypoints when we
        //reach them. Stops or starts our movement along the path, depending on 
        //our position in the list.
        agent.addControl(new PhysicsAgentControl());
        //this is a physics agent so add them to the physics space.
        getStateManager().getState(BulletAppState.class).getPhysicsSpace().add(agent);
        //Add the agent to the scene.
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
        DebugMoveControl dmc = new DebugMoveControl(crowd, createAgent, haloGeom.clone());
        dmc.setVisual(true); 
        dmc.setVerbose(false);                    
        agent.addControl(dmc);
    }
}
