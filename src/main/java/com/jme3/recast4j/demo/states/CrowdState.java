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
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.recast4j.Detour.Crowd.Crowd;
import com.jme3.recast4j.Detour.Crowd.Impl.CrowdManagerAppstate;
import com.jme3.recast4j.Detour.Crowd.MovementApplicationType;
import com.jme3.recast4j.demo.controls.CrowdBCC;
import com.jme3.recast4j.demo.controls.DebugMoveControl;
import com.jme3.recast4j.demo.controls.PhysicsAgentControl;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Torus;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
    
    private HashMap<String, NavMeshQuery> mapCrowds;;
    private Map<String, Grid> mapGrids;
    private boolean newGrid;
    private boolean checkGrids;    
    
    public CrowdState() {
        this.mapCrowds = new HashMap();
    }
    
    @Override
    protected void initialize(Application app) {
        //It is technically safe to do all initialization and cleanup in the 
        //onEnable()/onDisable() methods. Choosing to use initialize() and 
        //cleanup() for this is a matter of performance specifics for the 
        //implementor.
        //TODO: initialize your AppState, e.g. attach spatials to rootNode
    }

    @Override
    protected void cleanup(Application app) {
        Iterator<Map.Entry<String, Grid>> iterator = mapGrids.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Grid> entry = iterator.next();
            List<Node> agents = entry.getValue().getListAgents();
            for (Node agent: agents) {
                //Convolouted crap just to get a PhysicsRigidBody from BCC.
                if (agent.getControl(BetterCharacterControl.class) != null) {
                    PhysicsRigidBody prb = agent.getControl(CrowdBCC.class).getPhysicsRigidBody();
                    if (getStateManager().getState(BulletAppState.class).getPhysicsSpace().getRigidBodyList().contains(prb)) {
                        getStateManager().getState(BulletAppState.class).getPhysicsSpace().remove(agent);
                    }
                }
                ((SimpleApplication) getApplication()).getRootNode().detachChild(agent);
            }
            iterator.remove();
        }
    }

    //onEnable()/onDisable() can be used for managing things that should 
    //only exist while the state is enabled. Prime examples would be scene 
    //graph attachment or input listener attachment.
    @Override
    protected void onEnable() {
        MeshSetReader msr = new MeshSetReader();
        try {
            //Read in the saved navMesh with same maxVertPerPoly(3) saved. Will 
            //be added to mapCrowds as a key using the text returned by 
            //fieldCrowdName.
            NavMesh navMesh = msr.read(new FileInputStream("test.nm"), 3);
            //Create the query object for pathfinding in this Crowd. Will be 
            //added to the mapCrowds as a value so each crowd query object is
            //referenced.  
            NavMeshQuery query = new NavMeshQuery(navMesh);
            //Start crowd.
            Crowd crowd = new Crowd(MovementApplicationType.BETTER_CHARACTER_CONTROL, 100, .3f, navMesh);
            //Add to CrowdManager.
            getState(CrowdManagerAppstate.class).getCrowdManager().addCrowd(crowd);
            mapCrowds.put("Crowd", query);
            
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
    protected void onDisable() {
        //Called when the state was previously enabled but is now disabled 
        //either because setEnabled(false) was called or the state is being 
        //cleaned up.
    }
    
    @Override
    public void update(float tpf) {
        //Look for incactive grid to activate. Loads the agents into the physics
        //space and attaches them to the rootNode.
        if (newGrid) {
            mapGrids.forEach((key, value)-> {
                if (!value.isActiveGrid()) {
                    List<Node> agents = value.getListAgents();
                    for (Node agent: agents) {
                        //Physics agent so add to physics space.
                        if (agent.getControl(BetterCharacterControl.class) != null) {
                            getStateManager().getState(BulletAppState.class).getPhysicsSpace().add(agent);
                        }
                        ((SimpleApplication) getApplication()).getRootNode().attachChild(agent);
                    }
                    //Stop the the newGrid check from adding this again.
                    value.setActiveGrid(true);
                }
            });
            //All grids are activated so stop looking.
            newGrid = false;
        }
        
        //Look for grids to remove. Removes the agents from the root node and 
        //physics space. Use iterator to avoid ConcurrentModificationException.
        if (checkGrids) {
            Iterator<Map.Entry<String, Grid>> iterator = mapGrids.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, Grid> entry = iterator.next();
                if (entry.getValue().isRemoveGrid()) {
                    List<Node> agents = entry.getValue().getListAgents();
                    for (Node agent: agents) {
                        //Convolouted crap just to get a PhysicsRigidBody from BCC.
                        if (agent.getControl(BetterCharacterControl.class) != null) {
                            PhysicsRigidBody prb = agent.getControl(CrowdBCC.class).getPhysicsRigidBody();
                            if (getStateManager().getState(BulletAppState.class).getPhysicsSpace().getRigidBodyList().contains(prb)) {
                                getStateManager().getState(BulletAppState.class).getPhysicsSpace().remove(agent);
                            }
                        }
                        ((SimpleApplication) getApplication()).getRootNode().detachChild(agent);
                    }
                    iterator.remove();
                }
            }
            checkGrids = false;
        }        
    }
    
    /**
     * Adds an agent grid to the specified crowd but does not set the target.
     * @param crowdIndex
     * @param ap
     * @param gridName
     * @param updateFlags
     * @param debug
     */
    public void addAgentCrowd(int crowdIndex, CrowdAgentParams ap, String gridName, boolean debug) {

        
        List<Node> listAgents = getAgentList(gridName);
        Crowd crowd = getState(CrowdManagerAppstate.class).getCrowdManager().getCrowd(crowdIndex);

        for (Node agent: listAgents) {
            
            //Add agents to the crowd.
            CrowdAgent createAgent = crowd.createAgent(agent.getWorldTranslation(), ap);
            crowd.setSpatialForAgent(createAgent, agent);
                        
            if (debug) {
                Torus halo = new Torus(16, 16, 0.1f, 0.3f);
                Geometry haloGeom = new Geometry("halo", halo);
                Material haloMat = new Material(getApplication().getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
                haloMat.setColor("Color", ColorRGBA.Cyan);
                haloGeom.setMaterial(haloMat);
                haloGeom.setLocalTranslation(0, 2, 0);
                Quaternion pitch90 = new Quaternion();
                pitch90.fromAngleAxis(FastMath.PI/2, new Vector3f(1,0,0));
                haloGeom.setLocalRotation(pitch90);

                DebugMoveControl catControl = new DebugMoveControl(crowd, createAgent, haloGeom);
                agent.addControl(catControl);

            } else {
                if (agent.getControl(DebugMoveControl.class) != null) {
                    agent.removeControl(DebugMoveControl.class);
                } 
            }
            
            //anything over 2 arguments creates a new object so split this up.
            LOG.info("<===== BEGIN AgentParamState addAgentCrowd =====>");
            LOG.info("Crowd                 [{}]", crowdIndex);
            LOG.info("Active Agents         [{}]", crowd.getActiveAgents().size());
            LOG.info("Agent Name            [{}]", agent.getName());
            LOG.info("Position World        [{}]", agent.getWorldTranslation());
            LOG.info("Position Local        [{}]", agent.getLocalTranslation());
            LOG.info("radius                [{}]", ap.radius);
            LOG.info("height                [{}]", ap.height);
            LOG.info("maxAcceleration       [{}]", ap.maxAcceleration);
            LOG.info("maxSpeed              [{}]", ap.maxSpeed);
            LOG.info("colQueryRange         [{}]", ap.collisionQueryRange);
            LOG.info("pathOptimizationRange [{}]", ap.pathOptimizationRange);
            LOG.info("separationWeight      [{}]", ap.separationWeight);
            LOG.info("obstacleAvoidanceType [{}]", ap.obstacleAvoidanceType);
            LOG.info("updateFlags           [{}]", ap.updateFlags);
            LOG.info("<===== End AgentParamState addAgentCrowd =====>");
        }

    }    
    
    /**
     * Set the grid parameters for this grid. 
     * 
     * If checkPhysics is checked, it's expected that physics is to be used for 
     * navigation movement and a PhysicsAgentControl and BetterCharacterControl 
     * will be added to the spatial of choice. 
     * 
     * If either checkRadius or checkHeight is left unchecked, the value will be 
     * taken from the world bounds of the spatial for that attribute. Auto 
     * generation of radius and height is based off model bounds. For radius, 
     * this is the smallest value in the x or z direction / 2. For height, this 
     * would be the Y value * 2.
     * 
     * If weight is left unchecked, a default weight of 1.0f will be assigned.
     * 
     * If checkPhysics is left unchecked, a CrowdBCC will be used. The Radius 
     * and height of the spatial is determined as noted above except weight is ignored.
     * 
     * @param agentPath The Path of the agent to be used for this grid.
     * @param size The size of the grid to be created.
     * @param distance The spacing between agents in the grid.
     * @param startPos The start position of the agent. This has no other use 
     * outside of initial grid generation. 
     * @param gridName The name for this grid. This will also used when applying 
     * Obstacle Avoidance Parameters to the crowd.
     */          
    private void addAgentGrid(String agentPath, int size, float distance, 
            Vector3f startPos, String gridName, float radius, float height, 
            float weight, boolean physics) {

        //Anything over 2 arguments creates a new object so split this up.
        LOG.info("<===== Begin AgentGridState addAgentGrid =====>");
        LOG.info("agentPath         [{}]", agentPath);
        LOG.info("size              [{}]", size);
        LOG.info("separation        [{}]", distance);
        LOG.info("startPos          [{}]", startPos);
        LOG.info("gridName          [{}]", gridName);


        List<Node> listAgents = new ArrayList<>(size*size);
        
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                
                LOG.info("<<<<<<<<<<     >>>>>>>>>>");

                Node agent = (Node) getApplication().getAssetManager().loadModel(agentPath);
                
                //Agent name.
                agent.setName(gridName + "_r" + i + "_c"+ j);
                LOG.info("Agent Name        [{}]", agent.getName());
                              
                //Set the start position for each spatial
                float startX = startPos.getX() + i * distance;
                float startY = startPos.getY();
                float startZ = startPos.getZ() + j * distance;
                Vector3f start = new Vector3f(startX, startY, startZ);
                agent.setLocalTranslation(start);
                
                //If checkPhysics, we use BCC and PhysicsAgentControl
                if (physics) {
                    //Give the agent physics controls controls. Will be added to
                    //physics space from update loop.
                    //PhysicsRigidBody from CrowdBCC is detectable for cleanup
                    //so had to extend it just to add the getter.
//                    agent.addControl(new BetterCharacterControl(radius, height, weight));
                    agent.addControl(new CrowdBCC(radius, height, weight));
                    agent.addControl(new PhysicsAgentControl());

                    LOG.info("weight            [{}]", weight);
                } 
                
                LOG.info("radius            [{}]", radius);
                LOG.info("height            [{}]", height);
                LOG.info("Position World    [{}]", agent.getWorldTranslation());
                LOG.info("Position Local    [{}]", agent.getLocalTranslation());
                //Add to agents list.
                listAgents.add(agent);
            }
        }
        //Create grid and add to the mapGrid. Tell the update loop to check for 
        //new grids to activate.
        LOG.info("listAgents size   [{}]", listAgents.size());
        Grid grid = new Grid(gridName, listAgents);
        addMapGrid(gridName, grid);
        newGrid = true;
        LOG.info("<===== End AgentGridState addAgentGrid =====>");
    }  
    
    /**
     * Returns true if mapGrids map contains the specified key.
     * 
     * @param key The key to look for in the mapGrids map.
     * @return The mapGrids
     */
    public boolean hasMapGrid(String key) {
        return mapGrids.containsKey(key);
    }
    
    /**
     * Add a new grid to the mapGrid.
     * 
     * @param key The key for the grid which is the name of the grid.
     * @param value The grid to be added to the mapGrid.
     */
    private void addMapGrid(String key, Grid value) {
        mapGrids.put(key, value);
    }
    
    
    /**
     * Removes a grid from mapGrids.
     * 
     * @param key The grid name to be removed.
     */
    public void removeGrid(String key) {
        mapGrids.get(key).setRemoveGrid(true);
        checkGrids = true;
    }
    
    /**
     * Grabs the agent list for the requested grid.
     * 
     * @param key The name of the grid to look for.
     * @return The list of agents for the supplied grid name.
     */
    public List<Node> getAgentList(String key) {
        return mapGrids.get(key).listAgents;
    }
    
    /**
     * The grid object for storing the grids. The grid name and listAgents are 
     * used to guarantee this is a unique grid for the value used for the hashmap.
     */
    private class Grid {

        private final String gridName;
        private final List<Node> listAgents;
        private boolean activeGrid;
        private boolean removeGrid;
        
        public Grid(String gridName, List<Node> listAgents) {
            this.gridName = gridName;
            this.listAgents = listAgents;
        }
        
        /**
         * If true, this grid is scheduled for removal in the next update.
         * 
         * @return the checkGrids
         */
        public boolean isRemoveGrid() {
            return removeGrid;
        }

        /**
         * A setting of true will trigger the removal of the grid. All agents 
         * associated with the grid will be removed from the physics space and
         * rootNode. Removal takes place in the next pass of the update loop.
         * 
         * @param removeGrid the checkGrids to set
         */
        public void setRemoveGrid(boolean removeGrid) {
            this.removeGrid = removeGrid;
        }
        
        /**
         * If grid is inactive it will be activated on the next update and all 
         * agents loaded into the rootNode and physics space from the update loop.
         * 
         * @return the activeGrid
         */
        public boolean isActiveGrid() {
            return activeGrid;
        }

        /**
         * A setting of true will keep the grid active in the mapGrid and prevent
         * the loading of this grids agents.
         * 
         * @param activeGrid the activeGrid to set
         */
        public void setActiveGrid(boolean activeGrid) {
            this.activeGrid = activeGrid;
        }

        /**
         * The name of this grid.
         * 
         * @return the gridName
         */
        public String getGridName() {
            return gridName;
        }

        /**
         * The list of agents to be used for this crowd grid.
         * 
         * @return the listAgents
         */
        public List<Node> getListAgents() {
            return listAgents;
        }

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 11 * hash + Objects.hashCode(this.gridName);
            hash = 11 * hash + Objects.hashCode(this.listAgents);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final Grid other = (Grid) obj;
            if (!Objects.equals(this.gridName, other.gridName)) {
                return false;
            }
            if (!Objects.equals(this.listAgents, other.listAgents)) {
                return false;
            }
            return true;
        }
        
        @Override
        public String toString() {
            return "Grid [name = "+ gridName + "] " + "AGENTS " + getListAgents(); 
        }

    }
}
