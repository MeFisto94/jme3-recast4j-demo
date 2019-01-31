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
import com.jme3.app.state.BaseAppState;
import com.jme3.bounding.BoundingBox;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.recast4j.Detour.BetterDefaultQueryFilter;
import com.jme3.recast4j.Detour.Crowd.Crowd;
import com.jme3.recast4j.Detour.Crowd.Impl.CrowdManagerAppstate;
import com.jme3.recast4j.Detour.DetourUtils;
import com.jme3.recast4j.demo.controls.DebugMoveControl;
import com.jme3.recast4j.demo.layout.MigLayout;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Torus;
import com.simsilica.lemur.ActionButton;
import com.simsilica.lemur.CallMethodAction;
import com.simsilica.lemur.Checkbox;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.GuiGlobals;
import com.simsilica.lemur.Label;
import com.simsilica.lemur.ListBox;
import com.simsilica.lemur.TextField;
import com.simsilica.lemur.event.PopupState;
import java.util.Arrays;
import java.util.List;
import org.recast4j.detour.FindNearestPolyResult;
import org.recast4j.detour.NavMeshQuery;
import org.recast4j.detour.crowd.CrowdAgent;
import org.recast4j.detour.crowd.CrowdAgentParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Holds the agent parameter panel components.
 * 
 * @author Robert
 */
public class AgentParamState extends BaseAppState {

    private static final Logger LOG = LoggerFactory.getLogger(AgentParamState.class.getName());
    
    private Container contAgentParams;
    private TextField fieldColQueryRange;
    private TextField fieldHeight;
    private TextField fieldMaxAccel;
    private TextField fieldMaxSpeed;
    private TextField fieldPathOptimizeRange;
    private TextField fieldRadius;
    private TextField fieldSeparationWeight;
    private TextField fieldTargetX;
    private TextField fieldTargetY;
    private TextField fieldTargetZ;
    private ListBox<Integer> listBoxAvoidance;
    private Checkbox checkAvoid;
    private Checkbox checkSep;
    private Checkbox checkTopo;
    private Checkbox checkTurns;
    private Checkbox checkVis;
    private Checkbox checkRadius;
    private Checkbox checkHeight;
    private Checkbox checkMoveRequest;

    
    @SuppressWarnings("unchecked")
    @Override
    protected void initialize(Application app) {
        
        //The top container for this gui.
        contAgentParams = new Container(new MigLayout("align center"));
        contAgentParams.setName("AgentParamState contAgentParams");
        contAgentParams.setAlpha(0, false);
        
        //The parmeters container.
        Container contParams = new Container(new MigLayout("wrap", "[grow]"));
        contParams.setName("AgentParamState contParams");
        contParams.setAlpha(0, false);
        contAgentParams.addChild(contParams, "wrap, growx");

        //Begin the crowd agent parameters section.
        contParams.addChild(new Label("Crowd Agent Parameters"));
        
        //The auto-generate radius checkbox.
        checkRadius = contParams.addChild(new Checkbox("Agent Radius"), "split 2, growx");
        fieldRadius = contParams.addChild(new TextField("0.6"));
        fieldRadius.setSingleLine(true);
        fieldRadius.setPreferredWidth(50);
        
        //The auto-generate height checkbox.
        checkHeight = contParams.addChild(new Checkbox("Agent Height"), "split 2, growx");
        fieldHeight = contParams.addChild(new TextField("2.0"));
        fieldHeight.setSingleLine(true);
        fieldHeight.setPreferredWidth(50);
        
        //The max acceleration field.
        contParams.addChild(new Label("Max Acceleration"), "split 2, growx");
        fieldMaxAccel = contParams.addChild(new TextField("8.0"));
        fieldMaxAccel.setSingleLine(true);
        fieldMaxAccel.setPreferredWidth(50);
        
        //The max speed field.
        contParams.addChild(new Label("Max Speed"), "split 2, growx");
        fieldMaxSpeed = contParams.addChild(new TextField("3.5"));
        fieldMaxSpeed.setSingleLine(true);
        fieldMaxSpeed.setPreferredWidth(50);
        
        //The collision query range.
        contParams.addChild(new Label("Collision Query Range"), "split 2, growx");
        fieldColQueryRange = contParams.addChild(new TextField("12.0"));
        fieldColQueryRange.setSingleLine(true);
        fieldColQueryRange.setPreferredWidth(50);
        
        //The path optimization range.
        contParams.addChild(new Label("Path Optimize Range"), "split 2, growx");
        fieldPathOptimizeRange = contParams.addChild(new TextField("30.0"));
        fieldPathOptimizeRange.setSingleLine(true);
        fieldPathOptimizeRange.setPreferredWidth(50);
        
        //The separation weight.
        contParams.addChild(new Label("Separation Weight"), "split 2, growx");
        fieldSeparationWeight = contParams.addChild(new TextField("2.0"));
        fieldSeparationWeight.setSingleLine(true);
        fieldSeparationWeight.setPreferredWidth(50);
        


        Container contAvoidance = new Container(new MigLayout("wrap"));
        contAvoidance.setName("AgentParamState contAvoidance");
        contAvoidance.setAlpha(0, false);
        contAgentParams.addChild(contAvoidance, "split 2");
        
        //The avoidance label.
        contAvoidance.addChild(new Label("Avoidance Type"));
        
        //Obstacle avoidance listbox.
        listBoxAvoidance = contAvoidance.addChild(new ListBox<>(), "align 50%");
        listBoxAvoidance.setVisibleItems(7);
        //Have to set this here since Crowd has package-private access the to 
        //the DT_CROWD_MAX_OBSTAVOIDANCE_PARAMS variable. Currently this is eight.
        for (int i = 0; i < 8; i++) {
            listBoxAvoidance.getModel().add(i);
        }
        listBoxAvoidance.getSelectionModel().setSelection(0);
        
        
        
        //Update flags.
        Container contUpdateFlags = new Container(new MigLayout("wrap"));
        contUpdateFlags.setName("AgentParamState contUpdateFlags");
        contUpdateFlags.setAlpha(0, false);
        contAgentParams.addChild(contUpdateFlags, "wrap, growy");
        
        contUpdateFlags.addChild(new Label("Update Flags"));
        checkTurns = contUpdateFlags.addChild(new Checkbox("ANTICIPATE_TURNS"));        
        checkAvoid = contUpdateFlags.addChild(new Checkbox("OBSTACLE_AVOIDANCE"));
        checkTopo = contUpdateFlags.addChild(new Checkbox("OPTIMIZE_TOPO"));
        checkVis = contUpdateFlags.addChild(new Checkbox("OPTIMIZE_VIS"));
        checkSep = contUpdateFlags.addChild(new Checkbox("SEPARATION"));
        
        
        
        //Container that holds the start position components.
        Container contTarget = new Container(new MigLayout(null, "[grow]"));
        contTarget.setName("CrowdBuilderState contTarget");
        contTarget.setAlpha(0, false);
        contAgentParams.addChild(contTarget, "wrap, growx");
        
        //The start postion field.
        contTarget.addChild(new Label("Target Position"), "wrap"); 
        //X
        contTarget.addChild(new Label("X:"), "split 6");
        fieldTargetX = contTarget.addChild(new TextField("0.0"));
        fieldTargetX.setSingleLine(true);
        fieldTargetX.setPreferredWidth(75);
        //Y
        contTarget.addChild(new Label("Y:"));
        fieldTargetY = contTarget.addChild(new TextField("0.0"));
        fieldTargetY.setSingleLine(true);
        fieldTargetY.setPreferredWidth(75);
        //Z        
        contTarget.addChild(new Label("Z:"));
        fieldTargetZ = contTarget.addChild(new TextField("0.0"), "wrap");
        fieldTargetZ.setSingleLine(true);
        fieldTargetZ.setPreferredWidth(75);
        
        //Set the target for the crowd.
        contTarget.addChild(new ActionButton(new CallMethodAction("Set Target", this, "setTarget")), "split 2");
        //The movement request halo checkbox.
        checkMoveRequest = contTarget.addChild(new Checkbox("Debug Movement"), "gap left push");
        checkMoveRequest.getModel().setChecked(true);
        
        
        //Holds the Legend and Setup buttons.
        Container contButton = new Container(new MigLayout(null, // Layout Constraints
                "[]push[][]")); // Column constraints [min][pref][max]
        contButton.setName("AgentParamState contButton");
        contButton.setAlpha(1, false);
        contAgentParams.addChild(contButton, "growx");
        
        //Buttons.
        contButton.addChild(new ActionButton(new CallMethodAction("Help", this, "showHelp")));
        contButton.addChild(new ActionButton(new CallMethodAction("Add Agents Crowd", this, "addAgentCrowd")));        


    }

    @Override
    protected void cleanup(Application app) {
        //The removal of the gui components is a by product of the removal of 
        //CrowdBuilderState where this gui lives.
    }

    /**
     * Called by AgentGridState(onEnable). CrowdBuilderState needs 
     * AgentGridState and AgentParamState to build its gui. This is the middle 
     * of the attachment chain. 
     * AgentGridState(onEnable)=>AgentParamState(onEnable)=>CrowdBuilderState(onEnable)
     */
    @Override
    protected void onEnable() {
        getStateManager().attach(new CrowdBuilderState());
    }

    /**
     * Called by CrowdBuilderState(onDisable) as part of a chain detachment of states. 
     * This is the middle of the detachment chain. Lemur cleanup for all states 
     * is done from CrowdBuilderState.
     * CrowdBuilderState(onDisable)=>AgentParamState(onDisable)=>AgentGridState(onDisable)
     */
    @Override
    protected void onDisable() {
        if (getStateManager().hasState(getState(AgentGridState.class))) {
            getStateManager().getState(AgentGridState.class).setEnabled(false);
        }
        getStateManager().detach(this);
    }
    
    @Override
    public void update(float tpf) {
        //TODO: implement behavior during runtime
    }
    
    /**
     * Explains the agent parameters.
     */
    private void showHelp() {

        String[] msg = {
        "Agent Radius - The radius of the agent. When presented with an opening  they are to", 
        "large to enter, pathFinding will try to navigate around it. If checked, the given value will", 
        "be used for the radius of Crowd navigation. Left unchecked, and the radius assigned to", 
        "the agent during the grid creation process will be used instead. [Limit: >= 0]",
        " ",
        "Agent Height - The height of the agent. Obstacles with a height less than this ",
        "(value - radius) will cause pathFinding to try and find a navigable path around the", 
        "obstacle. If checked, the given value will be used for the height of Crowd navigation. Left", 
        "unchecked, and the height assigned to the agent during the grid creation process will be",
        "used instead. [Limit: > 0]",
        " ",
        "Max Acceleration - When an agent lags behind in the path, this is the maximum burst of", 
        "speed the agent will move at when trying to catch up to their expected position.",
        "[Limit: >= 0]",
        " ",
        "Max Speed - the maximum speed the agent will travel along the path when",
        "unobstructed. [Limit: >= 0]",
        " ",
        "Collision Query Range - Defines how close a collision element must be before it's", 
        "considered for steering behaviors. [Limits: > 0]",
        " ",
        "Path Optimization Range: The path visibility optimization range. [Limit: > 0]",
        " ",
        "Separation Weight - How aggressive  the agent manager should be at avoiding collisions", 
        "with this agent. [Limit: >= 0]",
        " ",
        "Avoidance Type - This is the Obstacle Avoidance configuration to be applied to this", 
        "agent. Currently, the max number of avoidance types that can be configured for the", 
        "Crowd is eight. See [ Crowd ] [ Obstacle Avoidance Parameters ]. [Limits: 0 <= value < 8]",
        " ",
        "Update Flags - Crowd agent update flags. This is a required setting.",
        " ",
        "Target Position - This is the target for the crowd to move to. You can set it manually or", 
        "by hovering your mouse pointer over the desired target and selecting the [ Shift ] key.",
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
     * Adds an agent to the specified crowd but does not set the target.
     */
    private void addAgentCrowd() {

        float radius;
        float height;
        float maxAccel;
        float maxSpeed;   
        float colQueryRange;
        float pathOptimizeRange;
        float separationWeight;
        int updateFlags;
        int obstacleAvoidanceType;
        
        Integer selectedCrowd = getState(CrowdBuilderState.class).getSelectedCrowd();
        
        //Must select a crowd before anything else.
        if (selectedCrowd == null) {
            displayMessage("You must select a [ Active Crowd ] from the [ Crowd ] tab.", 0); 
            return;
        }
        
        //Must select a agent grid.
        //Get the selectedAgentGrid from listBoxGrid.
        Integer selectedAgentGrid = getState(AgentGridState.class)
                .getListBoxGrid().getSelectionModel().getSelection();

        //Check to make sure a grid has been selected.
        if (selectedAgentGrid == null) {
            displayMessage("You must select a [ Active Grid ] from the [ Agent Grid ] tab.", 0);
            return;
        }

        //Get the grids name from the listBoxGrid selectedAgentGrid.
        String gridName = getState(AgentGridState.class)
                .getListBoxGrid().getModel().get(selectedAgentGrid).toString();

        //We check mapGrids to see if the key exists. If not, go no further.
        if (!getState(AgentGridState.class).hasMapGrid(gridName)) {
            displayMessage("No grid found by that name.", 0);
            return;
        }

        //The agent radius. 
        if (checkRadius.isChecked()) {
            if (fieldRadius.getText().isEmpty()
            || !getState(GuiUtilState.class).isNumeric(fieldRadius.getText())) {
                displayMessage("[ Agent Radius ] requires a valid float value.", 0);
                return;
            } 
        } 

        //The agent height. If empty we will use auto generated settings gathered
        //when the agent was added to its grid in the Add Grid tab.
        if (checkHeight.isChecked()) {
            if (fieldHeight.getText().isEmpty()
            || !getState(GuiUtilState.class).isNumeric(fieldHeight.getText())) {
                displayMessage("[ Agent Height ] requires a valid float value.", 0);
                return;
            } 
        }

        //The max acceleration settings.
        if (!getState(GuiUtilState.class).isNumeric(fieldMaxAccel.getText()) 
        ||  fieldMaxAccel.getText().isEmpty()) {
            displayMessage("[ Max Acceleration ] requires a valid float value.", 0);
            return;
        } else {
            maxAccel = new Float(fieldMaxAccel.getText());
            //Stop negative input.
            if (maxAccel < 0.0f) {
                displayMessage("[ Max Acceleration ] requires a float value >= 0.0f.", 0);
                return;
            }
        }

        //The max speed settings.
        if (!getState(GuiUtilState.class).isNumeric(fieldMaxSpeed.getText()) 
        ||  fieldMaxSpeed.getText().isEmpty()) {
            displayMessage("[ Max Speed ] requires a valid float value.", 0);
            return;
        } else {
            maxSpeed = new Float(fieldMaxSpeed.getText());
            //Stop negative input.
            if (maxSpeed < 0.0f) {
                displayMessage("[ Max Speed ] requires a float value >= 0.0f.", 0);
                return;
            }
        }

        //The collision query range.
        if (!getState(GuiUtilState.class).isNumeric(fieldColQueryRange.getText()) 
        ||  fieldColQueryRange.getText().isEmpty()) {
            displayMessage("[ Collision Query Range ] requires a valid float value.", 0);
            return;
        } else {
            colQueryRange = new Float(fieldColQueryRange.getText());
            //Stop negative input.
            if (colQueryRange <= 0.0f) {
                displayMessage("[ Collision Query Range ] requires a float value > 0.0f.", 0);
                return;
            }
        }

        //The path optimize range.
        if (!getState(GuiUtilState.class).isNumeric(fieldPathOptimizeRange.getText()) 
        ||  fieldPathOptimizeRange.getText().isEmpty()) {
            displayMessage("[ Path Optimize Range ] requires a valid float value.", 0);
            return;
        } else {
            pathOptimizeRange = new Float(fieldPathOptimizeRange.getText());
            //Stop negative input.
            if (pathOptimizeRange <= 0.0f) {
                displayMessage("[ Path Optimize Range ] requires a float value > 0.0f.", 0);
                return;
            }
        }

        //The separation weight settings.
        if (!getState(GuiUtilState.class).isNumeric(fieldSeparationWeight.getText()) 
        ||  fieldSeparationWeight.getText().isEmpty()) {
            GuiGlobals.getInstance().getPopupState()
                    .showModalPopup(getState(GuiUtilState.class)
                            .buildPopup("[ Separation Weight ] requires a valid float value.", 0));
            return;
        } else {
            separationWeight = new Float(fieldSeparationWeight.getText());
            //Stop negative input.
            if (separationWeight < 0.0f) {
                GuiGlobals.getInstance().getPopupState()
                        .showModalPopup(getState(GuiUtilState.class)
                                .buildPopup("[ Separation Weight ] requires a float value >= 0.0f.", 0));
                return;
            }
        }

        //The update flags settings.
        if (!checkTurns.isChecked() 
        &&  !checkAvoid.isChecked() 
        &&  !checkTopo.isChecked() 
        &&  !checkVis.isChecked() 
        &&  !checkSep.isChecked()) {
            displayMessage("Select at least one [ Update Flag ].", 0);
            return;
        } else {
            updateFlags = 0;
            if (checkTurns.isChecked()) {
                updateFlags += CrowdAgentParams.DT_CROWD_ANTICIPATE_TURNS;
            }

            if (checkAvoid.isChecked()) {
                updateFlags += CrowdAgentParams.DT_CROWD_OBSTACLE_AVOIDANCE;
            }

            if (checkTopo.isChecked()) {
                updateFlags += CrowdAgentParams.DT_CROWD_OPTIMIZE_TOPO;
            }

            if (checkVis.isChecked()) {
                updateFlags += CrowdAgentParams.DT_CROWD_OPTIMIZE_VIS;
            }

            if (checkSep.isChecked()) {
                updateFlags += CrowdAgentParams.DT_CROWD_SEPARATION;
            }
        }

        //Obstacle Avoidance Type. Selection is set to 0 when creating 
        //the listBoxAvoidance so shouldn't need to check for null or
        //parameter configurations less than 0. 
        obstacleAvoidanceType = listBoxAvoidance.getSelectionModel().getSelection();        
        
        //Everything checks out so far so grab the selected list of agents for 
        //the grid.
        List<Node> listAgents = getState(AgentGridState.class).getAgentList(gridName);
        Crowd crowd = getState(CrowdManagerAppstate.class).getCrowdManager().getCrowd(selectedCrowd);

        if (listAgents.size() > crowd.getAgentCount()) {
            displayMessage(
                      "Agent grid size of [" + listAgents.size() + "] excedes the crowd size ["
                    + crowd.getAgentCount() + "].", 0);
            return;
        } else if ((listAgents.size() + crowd.getActiveAgents().size()) > crowd.getAgentCount()) {
            displayMessage(
                      "Agent grid size of [" + listAgents.size() + "] plus active agents of [" 
                    + crowd.getActiveAgents().size() + "] excedes the crowd size ["
                    + crowd.getAgentCount() + "].", 0);
            return;
        }
                
        //If checked, we use the fieldRadius for the radius.
        if (checkRadius.isChecked()) {
            radius = new Float(fieldRadius.getText());
            //Stop negative input.
            if (radius < 0.0f) {
                displayMessage("[ Agent Radius ] requires a float value >= 0.", 0);
                return;
            }
        } else {
            //Auto calculate based on bounds.
            BoundingBox bounds = (BoundingBox) listAgents.get(0).getWorldBound();
            float x = bounds.getXExtent();
            float z = bounds.getZExtent();

            float xz = x < z ? x:z;
            radius = xz/2;
        }

        //If checked, we use the fieldHeight for height.
        if (checkHeight.isChecked()) {
            height = new Float(fieldHeight.getText());
            //Stop negative input.
            if (height <= 0.0f) {
                displayMessage("[ Agent Height ] requires a float value > 0.", 0);
                return;
            }
        } else {
            //Auto calculate based on bounds.
            BoundingBox bounds = (BoundingBox) listAgents.get(0).getWorldBound();
            float y = bounds.getYExtent();
            height = y*2;
        }
        
        //Build the params object.
        CrowdAgentParams ap = new CrowdAgentParams();
        ap.radius                   = radius;
        ap.height                   = height;
        ap.maxAcceleration          = maxAccel;
        ap.maxSpeed                 = maxSpeed;
        ap.collisionQueryRange      = colQueryRange;
        ap.pathOptimizationRange    = pathOptimizeRange;
        ap.separationWeight         = separationWeight;
        ap.updateFlags              = updateFlags;
        ap.obstacleAvoidanceType    = obstacleAvoidanceType;
        
        //Temp fix for crowd clearing.
        resetCrowd(selectedCrowd);
        
        for (Node agent: listAgents) {
            
            //Add agents to the crowd.
            CrowdAgent createAgent = crowd.createAgent(agent.getWorldTranslation(), ap);
            crowd.setSpatialForAgent(createAgent, agent);
                        
            if (checkMoveRequest.isChecked() ) {
                if (agent.getControl(DebugMoveControl.class) == null) {
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
                    agent.getControl(DebugMoveControl.class).setEnabled(true);
                }

            } else {
                if (agent.getControl(DebugMoveControl.class) != null) {
                    agent.getControl(DebugMoveControl.class).setEnabled(false);
                } 
            }
            
            //anything over 2 arguments creates a new object so split this up.
            LOG.info("<===== BEGIN AgentParamState addAgentCrowd =====>");
            LOG.info("Crowd                 [{}]", selectedCrowd);
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
     * Set the target for the selected crowd.
     */
    private void setTarget() {

        int selectedCrowd = getState(CrowdBuilderState.class).getSelectedCrowd();
        
        //Check to make sure a crowd has been selected.
        if (selectedCrowd == -1) {
            displayMessage("Select an [ Active Crowd ] from the [ Crowd ] tab to set the target for.", 0); 
            return;
        } 
        
        //The target position of the grid. Sanity check.
        if (!getState(GuiUtilState.class).isNumeric(fieldTargetX.getText()) || fieldTargetX.getText().isEmpty() 
        ||  !getState(GuiUtilState.class).isNumeric(fieldTargetY.getText()) || fieldTargetY.getText().isEmpty() 
        ||  !getState(GuiUtilState.class).isNumeric(fieldTargetZ.getText()) || fieldTargetZ.getText().isEmpty()) {
            displayMessage("[ Start Position ] requires a valid float value.", 0);
        } else {
            Float x = new Float(fieldTargetX.getText());
            Float y = new Float(fieldTargetY.getText());
            Float z = new Float(fieldTargetZ.getText());
            Vector3f target = new Vector3f(x, y, z);
            Crowd crowd = getState(CrowdManagerAppstate.class).getCrowdManager().getCrowd(selectedCrowd);
            
            //Get the query extent for this crowd.
            float[] ext = crowd.getQueryExtents();
            
            //Agents in the crowd.
            int agentCount = crowd.getAgentCount();
            
            //Get the query object.
            NavMeshQuery query = getState(CrowdBuilderState.class).getQuery();
        
            if (query == null) {
               displayMessage("Query object not found. Select an "
                       + "[ Active Crowd ] from the [ Crowd ] tab first.", 0);  
               return;
            }

            LOG.info("<========== BEGIN AgentParamState setTarget ==========>");
            LOG.info("queryExt              [{}]", ext);
            LOG.info("setTarget             [{}]", target);
            //Locate the nearest poly ref/pos.
            FindNearestPolyResult nearest = query.findNearestPoly(DetourUtils.toFloatArray(target), ext, new BetterDefaultQueryFilter());
            //Set the target.
//            for (int i = 0; i < agentCount; i++) {
//                CrowdAgent ag = getState(CrowdManagerAppstate.class).getCrowdManager().getCrowd(selectedCrowd).getAgent(i);
//                if (!ag.isActive()) {
//                    continue;
//                }
//            
//                getState(CrowdManagerAppstate.class).getCrowdManager().getCrowd(selectedCrowd)
//                        .requestMoveTarget(i, nearest.getNearestRef(), nearest.getNearestPos());
//            }

            LOG.info("nearesPos             [{}] nearestRef [{}]", nearest.getNearestPos(), nearest.getNearestRef());
            if (nearest.getNearestRef() == 0) {
                LOG.info("getNearestRef() can't be 0. ref [{}]", nearest.getNearestRef());
            } else {
                crowd.requestMoveToTarget(DetourUtils.createVector3f(nearest.getNearestPos()), nearest.getNearestRef());
            }
            
            LOG.info("<========== END AgentParamState setTarget ==========>");
        }
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
     * Resets all crowd agent move targets for the selected crowd. 
     * targetRef = 0;
     * targetPos( 0, 0, 0);
     * dvel( 0, 0, 0);
     * targetPathqRef = PathQueue.DT_PATHQ_INVALID;
     * targetReplan = false;
     * targetState = MoveRequestState.DT_CROWDAGENT_TARGET_NONE;
     * @param crowd The crowd to reset.
     */
    private void resetCrowd(int crowd) {
        Crowd crowd1 = getState(CrowdManagerAppstate.class).getCrowdManager().getCrowd(crowd);
        List<CrowdAgent> activeAgents = crowd1.getActiveAgents();
        for (CrowdAgent agent: activeAgents) {
            crowd1.resetMoveTarget(activeAgents.indexOf(agent));
            crowd1.removeAgent(agent);
        }
    }
    
    /**
     * @return The contAgentParams.
     */
    public Container getContAgentParams() {
        return contAgentParams;
    }
    
    /**
     * Sets the target by converting vector3f to string.
     * 
     * @param target The requested target value to set.
     */
    public void setFieldTargetXYZ(Vector3f target) {
        String x = "" + target.x;
        String y = "" + target.y;
        String z = "" + target.z;
        this.fieldTargetX.setText(x);
        this.fieldTargetY.setText(y);
        this.fieldTargetZ.setText(z);
    }    
    
}
