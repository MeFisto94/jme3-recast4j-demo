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
import com.jme3.bounding.BoundingBox;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.control.BetterCharacterControl;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.math.Vector3f;
import com.jme3.recast4j.demo.controls.CrowdBCC;
import com.jme3.recast4j.demo.controls.PhysicsAgentControl;
import com.jme3.recast4j.demo.layout.MigLayout;
import com.jme3.scene.Node;
import com.simsilica.lemur.ActionButton;
import com.simsilica.lemur.CallMethodAction;
import com.simsilica.lemur.Checkbox;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.GuiGlobals;
import com.simsilica.lemur.Label;
import com.simsilica.lemur.ListBox;
import com.simsilica.lemur.TextField;
import com.simsilica.lemur.event.PopupState;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Holds the agent grid panel components.
 * 
 * @author Robert
 */
public class AgentGridState extends BaseAppState {

    private static final Logger LOG = LoggerFactory.getLogger(AgentGridState.class.getName());

    private Container contAgentGrid;
    private Checkbox checkHeight;
    private Checkbox checkPhysics;
    private Checkbox checkRadius;
    private Checkbox checkWeight;
    private ListBox<String> listBoxAgent;
    private ListBox<String> listBoxGrid;
    private ListBox<Integer> listBoxSize;
    private TextField fieldDistance;
    private TextField fieldGridName;
    private TextField fieldHeight;
    private TextField fieldWeight;
    private TextField fieldPosX;
    private TextField fieldPosY;
    private TextField fieldPosZ;
    private TextField fieldRadius;
    private Map<String, Grid> mapGrids;
    private boolean newGrid;
    private boolean checkGrids;
    private float maxPopupSize;
    
    @SuppressWarnings("unchecked")
    @Override
    protected void initialize(Application app) {
        // Holds all active grids.
        mapGrids = new HashMap<>();
        //Set the maximum size a popup can be.
        maxPopupSize = 500.0f;
        
        //The top container for this gui.
        contAgentGrid = new Container(new MigLayout("align center"));
        contAgentGrid.setName("AgentGridState contBuildGridGui");
        contAgentGrid.setAlpha(0, false);

        //The container that holds agent stat selections.
        Container contStats = new Container(new MigLayout("wrap", "[grow, fill]"));
        contStats.setName("AgentGridState contStats");
        contStats.setAlpha(0, false);
        contAgentGrid.addChild(contStats, "split 2, growy");

        //The grid name field.
        contStats.addChild(new Label("Grid Name"), "split 2");
        fieldGridName = contStats.addChild(new TextField("Grid Jaime"), "growx");
        fieldGridName.setSingleLine(true);
        
        //The use physics checkbox.
        checkPhysics = contStats.addChild(new Checkbox("Use Physics"));
        checkPhysics.getModel().setChecked(true);

        //The Radius checkbox.
        checkRadius = contStats.addChild(new Checkbox("Agent Radius"), "split 2, growx");     
        fieldRadius = contStats.addChild(new TextField("0.6"));
        fieldRadius.setSingleLine(true);
        fieldRadius.setPreferredWidth(50);
                
        //The Height checkbox.
        checkHeight = contStats.addChild(new Checkbox("Agent Height"), "split 2, growx");
        fieldHeight = contStats.addChild(new TextField("2.0"));
        fieldHeight.setSingleLine(true);
        fieldHeight.setPreferredWidth(50);
        
        //The weight checkbox.
        checkWeight = contStats.addChild(new Checkbox("Agent Weight"), "split 2, growx");
        fieldWeight = contStats.addChild(new TextField("1.0"));
        fieldWeight.setSingleLine(true);
        fieldWeight.setPreferredWidth(50);
        
        //The crowd separation field.
        contStats.addChild(new Label("Agent Separation"), "split 2, growx");
        fieldDistance = contStats.addChild(new TextField("1.0"));
        fieldDistance.setSingleLine(true);
        fieldDistance.setPreferredWidth(50);
        


        //Container that holds the listbox for the models/agents.
        Container contAgent = new Container(new MigLayout("wrap", "[grow, fill]"));
        contAgent.setName("AgentGridState contAgent");
        contAgent.setAlpha(0, false);
        contAgentGrid.addChild(contAgent, "wrap, wmin 100");
        
        //The agentPath listbox. Add new model name here. Add the asset path to the 
        contAgent.addChild(new Label("Agent"));
        //models in butAddAgentGrid listener under the #Model Section.
        listBoxAgent = contAgent.addChild(new ListBox<>());
        listBoxAgent.getModel().add("Jamie");
        listBoxAgent.setVisibleItems(7);
        listBoxAgent.getSelectionModel().setSelection(0);
        
        
        
        //Container for the grid size listbox.
        Container contGridSize = new Container(new MigLayout("wrap"));
        contGridSize.setName("AgentGridState contSize");
        contGridSize.setAlpha(0, false);
        contAgentGrid.addChild(contGridSize, "split 2, growy");        
        
        //The grid size listbox. 
        contGridSize.addChild(new Label("Grid Size"));
        listBoxSize = contGridSize.addChild(new ListBox<>());
        listBoxSize.setVisibleItems(7);
        int size = 1;
        for (int i = 0; i < 15; i++) {
            listBoxSize.getModel().add(size);
            size++;
        }
        listBoxSize.getSelectionModel().setSelection(0);
        


        
        //Container that holds the active grid components.
        Container contGrid = new Container(new MigLayout("wrap", "[grow]"));
        contGrid.setName("AgentGridState contGrid");
        contGrid.setAlpha(0, false);
        contAgentGrid.addChild(contGrid, "wrap, growx, growy");
        
        //The active grids listbox.
        contGrid.addChild(new Label("Active Grids"));
        listBoxGrid = contGrid.addChild(new ListBox<>(), "growx");
        //Use the method call to set the checkGrids boolean to true.
        contGrid.addChild(new ActionButton(new CallMethodAction("Remove Grid", this, "removeGrid")));
                        


        //Holds all the underlying components for this gui.
        Container contStartPos = new Container(new MigLayout(null)); 
        contStartPos.setName("AgentGridState contStartPos");
        contStartPos.setAlpha(0, false);
        contAgentGrid.addChild(contStartPos, "wrap");
        
        //The start postion field.
        contStartPos.addChild(new Label("Start Position"), "wrap"); 
        //X
        contStartPos.addChild(new Label("X:"), "split 6");
        fieldPosX = contStartPos.addChild(new TextField("0.0"));
        fieldPosX.setSingleLine(true);
        fieldPosX.setPreferredWidth(50);
        //Y
        contStartPos.addChild(new Label("Y:"));
        fieldPosY = contStartPos.addChild(new TextField("0.0"));
        fieldPosY.setSingleLine(true);
        fieldPosY.setPreferredWidth(50);
        //Z        
        contStartPos.addChild(new Label("Z:"));
        fieldPosZ = contStartPos.addChild(new TextField("0.0"));
        fieldPosZ.setSingleLine(true);
        fieldPosZ.setPreferredWidth(50);
        
        
        
        //Holds the Add Agent and Legend buttons.
        Container contButton = new Container(new MigLayout(null, // Layout Constraints
                "[]push[][]")); // Column constraints [min][pref][max]
        contButton.setName("AgentGridState contButton");
        contButton.setAlpha(1, false);
        contAgentGrid.addChild(contButton, "growx");
        
        //Buttons.
        contButton.addChild(new ActionButton(new CallMethodAction("Help", this, "showHelp")));
        contButton.addChild(new ActionButton(new CallMethodAction("Add Agent Grid", this, "addGrid")));
        
    }

    /**
     * Removing this state from StateManager will clear all agents from the 
     * physics space and gui node. The removal of the gui components is a by
     * product of the removal of CrowdBuilderState where this gui lives.
     */
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

    /**
     * Called by the DemoApplication F1 button ActionListener. CrowdBuilderState needs 
     * AgentGridState and AgentParamState to build its gui. This is the start of 
     * the attachment chain. 
     * AgentGridState(onEnable)=>AgentParamState(onEnable)=>CrowdBuilderState(onEnable)
     */
    @Override
    protected void onEnable() {
        getStateManager().attach(new AgentParamState());
    }

    /**
     * Called by AgentParamState(onDisable) as part of a chain detachment of 
     * states. This is the end of the detachment chain. Lemur cleanup for all 
     * states is done from CrowdBuilderState.
     * CrowdBuilderState(onDisable)=>AgentParamState(onDisable)=>AgentGridState(onDisable)
     */
    @Override
    protected void onDisable() {
        getStateManager().detach(this);
    }
    
    @SuppressWarnings("unchecked")
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
                    listBoxGrid.getModel().add(key);
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
                    Integer selection = listBoxGrid.getSelectionModel().getSelection();
                    if (selection != null) {
                        listBoxGrid.getModel().remove((int) selection);
                    }
                }
            }
            checkGrids = false;
        }
        
    }
    
    /**
     * Explains the settings for the Grid Generator.
     */
    private void showHelp() {
        String[] msg = {
        "Grid Name - The name used for the grid. The agents used in the grid will all be named",
        "according to their insertion point into the grid of agents. This is a required setting.",
        " ",
        "Example: In a size 3 grid (3x3 = 9 agents), the naming is as follows.",
        " ",
        "r = row, c = column",
        "Row1 = gridName_r0_c0, gridName_r0_c1, gridName_r0_c2",
        "Row2 = gridname_r1_c0, gridName_r1_c1, gridName_r1_c2", 
        "Row2 = gridname_r2_c0, gridName_r2_c1, gridName_r2_c2", 
        " ",
        "Use Physics - If [ Use Physics ] is checked, it's expected that physics is to be used for", 
        "navigation movement and a PhysicsAgentControl and BetterCharacterControl will be", 
        "added to the Agent of choice.", 
        " ",
        "Agent Radius - The radius of the agent. Left unchecked, the value will be taken from the", 
        "world bounds of the spatial and is the smallest value in the x or z direction / 2.",
        " ",
        "Agent Height - The height of the agent. Left unchecked, the value will be taken from the", 
        "world bounds of the spatial and is the Y value * 2.",
        " ",
        "Agent Weight - The weight of the agent. Left unchecked, a default weight of 1.0f will be",
        "assigned if [ Use Physics ] is checked, otherwise, it is ignored.",
        " ",
        "Agent Separation - Spacing between agents in the grid. Must be a setting of 0.1f",
        "or larger.",
        " ",
        "Agent - Model to use for the grid.",
        " ",
        "Grid Size - Number of agents to place into the grid in rows and columns. Limit = 15", 
        "which is 225 agents.",
        " ",
        "Example: 2 = 2 rows, 2 columns = a grid of 4 agents.",
        " ",
        "Active Grids - The list of all the currently active grids. Select any grid from the list and",
        "press the [Remove Grid] button to remove the grid and all active agents related to that", 
        "grid. Each grid must have a unique name. Spaces count in the naming so if there is", 
        "added space after a grid name, that grid will be considered unique.",
        " ",
        "Start Position - Starting position the agents will spread out evenly from to form the grid.", 
        "This is only used to generate the agents for the grid so you can drop your agents from", 
        "above the navMesh if you wish. The actual starting point of the agent is determined by", 
        "its final position on the navMesh prior to being added to the crowd."
        };
        
        Container window = new Container(new MigLayout("wrap"));
        ListBox<String> listScroll = window.addChild(new ListBox<>());
        listScroll.getModel().addAll(Arrays.asList(msg));
        listScroll.setPreferredSize(new Vector3f(500, 400, 0));
        listScroll.setVisibleItems(20);
        window.addChild(new ActionButton(new CallMethodAction("Close", window, "removeFromParent")), "align 50%");
        getState(GuiUtilState.class).centerComp(window);
        //This assures clicking outside of the message should close the window 
        //but not activate underlying UI components.
        GuiGlobals.getInstance().getPopupState().showPopup(window, PopupState.ClickMode.ConsumeAndClose, null, null);
    }
    
    /**
     * Adds an agent grid to the world.
     */
    private void addGrid() {
        String gridName;
        float separation;
        String agentPath;
        Vector3f startPos;

        //Radius
        if (checkRadius.isChecked()) {
            //The agent radius. 
            if (fieldRadius.getText().isEmpty()
            || !getState(GuiUtilState.class).isNumeric(fieldRadius.getText())) {
                displayMessage("[ Agent Radius ] requires a valid float value.", 0);
                return;
            } 
        }
            
        //Height
        if (checkHeight.isChecked()) {
            //The agent height. 
            if (fieldHeight.getText().isEmpty()
            || !getState(GuiUtilState.class).isNumeric(fieldHeight.getText())) {
                displayMessage("[ Agent Height ] requires a valid float value.", 0);
                return;
            }
        }
        
        //Weight
        if (checkPhysics.isChecked() 
        &&  checkWeight.isChecked()) {
            //The agent weight. 
            if (fieldWeight.getText().isEmpty()
            || !getState(GuiUtilState.class).isNumeric(fieldWeight.getText())) {
                displayMessage("[ Agent Weight ] requires a valid float value.", 0);
                return;
            }
        }
         
        //Garbage in?, no grid.
        if (!getState(GuiUtilState.class).isNumeric(fieldDistance.getText()) 
        ||  fieldDistance.getText().isEmpty()) {
            displayMessage("[ Agent Separation ] requires a valid float value.", 0);
            return;
        } else {
            separation = new Float(fieldDistance.getText());
            //Stop negative separation input for agentPath grid.
            if (separation < 0.1f) {
                displayMessage("[ Agent Separation ] requires a float value >= 0.1f.", 0);
                return;
            }
        }

        //###### Model Section #####
        //Model to use for the grid. Selection is set to 0 when creating 
        //the listBoxAgent so shouldn't need to check for null.
        switch(listBoxAgent.getSelectionModel().getSelection()) {
            case 0: 
                agentPath = "Models/Jaime/Jaime.j3o";
                break;
            default: agentPath = null;
        }

        //Number of agents to place into the grid in rows and columns.
        //Selection is set when creating the listBoxSize so shouldn't need to 
        //check for null.
        int size = listBoxSize.getSelectionModel().getSelection() + 1;

        //The starting position of the grid. Sanity check.
        if (!getState(GuiUtilState.class).isNumeric(fieldPosX.getText()) || fieldPosX.getText().isEmpty() 
        ||  !getState(GuiUtilState.class).isNumeric(fieldPosY.getText()) || fieldPosY.getText().isEmpty() 
        ||  !getState(GuiUtilState.class).isNumeric(fieldPosZ.getText()) || fieldPosZ.getText().isEmpty()) {
            displayMessage("[ Start Position ] requires a valid float value.", 0);
            return;
        } else {
            Float x = new Float(fieldPosX.getText());
            Float y = new Float(fieldPosY.getText());
            Float z = new Float(fieldPosZ.getText());
            startPos = new Vector3f(x, y, z);
        }

        //The name of this grid.                
        if (fieldGridName.getText().isEmpty()) {
            displayMessage("You must enter a grid name.", 0);
            return;
        } else if (mapGrids.containsKey(fieldGridName.getText())) {
            displayMessage(
                      "[ " + fieldGridName.getText() 
                    + " ] has already been activated. "
                    + "Change the grid name or remove the existing grid before proceeding.", maxPopupSize);
            return;
        } else {
            gridName = fieldGridName.getText();
        }
                 
        addAgentGrid(agentPath, size, separation, startPos, gridName);
            
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
    private void addAgentGrid(String agentPath, int size, float distance, Vector3f startPos, String gridName) {

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
                //The agent radius, height.
                float radius;
                float height;
                
                //If checked, we use the fieldRadius for the radius.
                if (checkRadius.isChecked()) {
                    radius = new Float(fieldRadius.getText());
                    //Stop negative radius input.
                    if (radius < 0.0f) {
                        displayMessage("[ Agent Radius ] requires a float value >= 0.", 0);
                        listAgents = null;
                        return;
                    }
                } else {
                    //Auto calculate based on bounds.
                    BoundingBox bounds = (BoundingBox) agent.getWorldBound();
                    float x = bounds.getXExtent();
                    float z = bounds.getZExtent();

                    float xz = x < z ? x:z;
                    radius = xz/2;
                }
                
                //If checked, we use the fieldHeight for height.
                if (checkHeight.isChecked()) {
                    height = new Float(fieldHeight.getText());
                    //Stop negative height input.
                    if (height <= 0.0f) {
                        displayMessage("[ Agent Height ] requires a float value > 0.", 0);
                        listAgents = null;
                        return;
                    }
                } else {
                    //Auto calculate based on bounds.
                    BoundingBox bounds = (BoundingBox) agent.getWorldBound();
                    float y = bounds.getYExtent();
                    height = y*2;
                }               
                
                //Set the start position for each spatial
                float startX = startPos.getX() + i * distance;
                float startY = startPos.getY();
                float startZ = startPos.getZ() + j * distance;
                Vector3f start = new Vector3f(startX, startY, startZ);
                agent.setLocalTranslation(start);
                
                //If checkPhysics, we use BCC and PhysicsAgentControl
                if (checkPhysics.isChecked()) {
                    
                    float weight;
                    
                    //If checked, we use the fieldWeight for weight.
                    if (checkWeight.isChecked()) {
                        weight = new Float(fieldWeight.getText());
                        //Stop negative weight input.
                        if (weight <= 0.0f) {
                            displayMessage("[ Agent Weight ] requires a float value > 0.", 0);
                            listAgents = null;
                            return;
                        }
                    } else {
                        weight = 1.0f;
                    }
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
     * Add a new grid to the mapGrid.
     * 
     * @param key The key for the grid which is the name of the grid.
     * @param value The grid to be added to the mapGrid.
     */
    private void addMapGrid(String key, Grid value) {
        mapGrids.put(key, value);
    }
    
    /**
     * Displays a modal popup message.
     * 
     * @param txt The text for the popup.
     * @param width The maximum width for wrap. 
     */
    private void displayMessage(String txt, float width) {
        GuiGlobals.getInstance().getPopupState()
                    .showModalPopup(getState(GuiUtilState.class)
                            .buildPopup(txt, width));
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
    
    //Removes a grid from mapGrids.
    private void removeGrid() {
        
        //Get the selectedGrid from listBoxGrid.
        Integer selectedGrid = listBoxGrid.getSelectionModel().getSelection();
        
        //Check to make sure a grid has been selected.
        if (selectedGrid == null) {
            displayMessage("You must select a grid before it can be removed.", 0);
            return;
        }
        
        //Get the grids name from the listBoxGrid selectedGrid.
        String gridName = listBoxGrid.getModel().get(selectedGrid);
        
        //We check mapGrids to see if the key exists. If not, go no further.
        if (!mapGrids.containsKey(gridName)) {
            displayMessage("No grid found by that name.", 0);
            return;
        }
        
        //We have a valid grid so set the grid to be removed and tell the update
        //loop to look for grids to be removed.
        mapGrids.get(gridName).setRemoveGrid(true);
        checkGrids = true;
    }

    /**
     * Grabs the agent list for the requested grid.
     * 
     * @param gridName The name of the grid to look for.
     * @return The list of agents for the supplied grid name.
     */
    public List<Node> getAgentList(String gridName) {
        return mapGrids.get(gridName).listAgents;
    }
    
    /**
     * @return the contAgentGrid.
     */
    public Container getContAgentGrid() {
        return contAgentGrid;
    }

    /**
     * @return the listBoxGrid.
     */
    public ListBox getListBoxGrid() {
        return listBoxGrid;
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
