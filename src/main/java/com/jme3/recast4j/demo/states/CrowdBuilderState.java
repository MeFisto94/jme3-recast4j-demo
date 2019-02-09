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
import com.jme3.math.Vector3f;
import com.jme3.recast4j.Detour.Crowd.Crowd;
import com.jme3.recast4j.Detour.Crowd.CrowdManager;
import com.jme3.recast4j.Detour.Crowd.Impl.CrowdManagerAppstate;
import com.jme3.recast4j.Detour.Crowd.MovementApplicationType;
import com.jme3.recast4j.demo.controls.CrowdChangeControl;
import com.jme3.recast4j.demo.controls.DebugMoveControl;
import com.jme3.recast4j.demo.controls.PhysicsAgentControl;
import com.jme3.recast4j.demo.layout.MigLayout;
import com.jme3.recast4j.demo.states.AgentGridState.Grid;
import com.jme3.recast4j.demo.states.AgentGridState.GridAgent;
import com.simsilica.lemur.ActionButton;
import com.simsilica.lemur.CallMethodAction;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.GuiGlobals;
import com.simsilica.lemur.HAlignment;
import com.simsilica.lemur.Label;
import com.simsilica.lemur.ListBox;
import com.simsilica.lemur.RollupPanel;
import com.simsilica.lemur.TabbedPanel;
import com.simsilica.lemur.TextField;
import com.simsilica.lemur.core.VersionedReference;
import com.simsilica.lemur.event.CursorEventControl;
import com.simsilica.lemur.event.DragHandler;
import com.simsilica.lemur.event.PopupState;
import com.simsilica.lemur.list.DefaultCellRenderer;
import com.simsilica.lemur.style.ElementId;
import com.simsilica.lemur.text.DocumentModelFilter;
import com.simsilica.lemur.text.TextFilters;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.recast4j.detour.NavMesh;
import org.recast4j.detour.NavMeshQuery;
import org.recast4j.detour.crowd.CrowdAgent;
import org.recast4j.detour.crowd.ObstacleAvoidanceQuery.ObstacleAvoidanceParams;
import org.recast4j.detour.io.MeshSetReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Robert
 */
@SuppressWarnings("unchecked")
public class CrowdBuilderState extends BaseAppState {

    private static final Logger LOG = LoggerFactory.getLogger(CrowdBuilderState.class.getName());

    private Container contTabs;
    private DocumentModelFilter doc;
    private TextField fieldNavMesh;
    private TextField fieldMaxAgents;
    private TextField fieldMaxAgentRadius;
    private TextField fieldVelocityBias;
    private TextField fieldWeightDesVel;
    private TextField fieldWeightCurVel;
    private TextField fieldWeightSide;
    private TextField fieldWeightToi;
    private TextField fieldHorizTime;
    private TextField fieldGridSize;
    private TextField fieldAdaptiveRings;
    private TextField fieldAdaptiveDivs;
    private TextField fieldAdaptiveDepth;    
    private ListBox<String> listBoxAvoidance;    
    private ListBox<String> listBoxMoveType;
    private ListBox<Crowd> listBoxActiveCrowds;
    private ListBox<String> listBoxActiveGrids;
    private HashMap<Crowd, NavMeshQuery> mapCrowds;
    // Keep tracking crowd selected
    private VersionedReference<Set<Integer>> selectionRef; 
    // Keep tracking crowd selected
    private VersionedReference<List<Crowd>> modelRef;
    private String defaultOAPText;
    private String defaultActiveGridText;

    
    @Override
    protected void initialize(Application app) {
        
        //Holds a paired Crowd and NavMeshQuery object with Crowd key and Query
        //value.
        mapCrowds = new HashMap<>();
        
        //Displays when selectionRef and modelRef has changed and the model or 
        //selection is empty. Used as default for startup of listBoxAvoidance.
        defaultOAPText =                
                  "velBias              = n\\a\n"
                + "weightDesVel  = n\\a\n"
                + "weightCurVel   = n\\a\n"
                + "weightSide       = n\\a\n"
                + "weightToi         = n\\a\n"
                + "horizTime         = n\\a\n"
                + "gridSize            = n\\a\n"
                + "adaptiveDivs    = n\\a\n"               
                + "adaptiveRings  = n\\a\n"
                + "adaptiveDepth = n\\a";
        //Displays when selectionRef and modelRef has changed and the model or 
        //selection is empty. Used as default for startup of listBoxActiveCrowds.  
        defaultActiveGridText = "No Active Grids";
    }

    @Override
    protected void cleanup(Application app) {
        //Removing will also cleanup the AgentGridState and AgentParamState
        //lemur objects.
        ((SimpleApplication) getApplication()).getGuiNode().detachChild(contTabs);

        Iterator<Crowd> iterator = mapCrowds.keySet().iterator();
        while (iterator.hasNext()) {
            Crowd crowd = iterator.next();
            getState(CrowdManagerAppstate.class).getCrowdManager().removeCrowd(crowd);
            iterator.remove();
        }
    }

    /**
     * Called by AgentParamState(onEnable). CrowdBuilderState needs 
     * AgentGridState and AgentParamState to build its gui. This is the end of 
     * the attachment chain. 
     * AgentGridState(onEnable)=>AgentParamState(onEnable)=>CrowdBuilderState(onEnable)
     */
    @Override
    protected void onEnable() {
        
        //The top container for the crowd builder panel.
        Container contCrowd = new Container(new MigLayout("align center"));
        contCrowd.setName("CrowdBuilderState contCrowd");
        contCrowd.setAlpha(0, false);
        
        
        
        //Container that holds the parameters for starting the crowd.
        Container contCrowdParam = new Container(new MigLayout("wrap", "[grow]"));
        contCrowdParam.setName("CrowdBuilderState contCrowdParam");
        contCrowdParam.setAlpha(0, false);
        contCrowd.addChild(contCrowdParam, "growx, growy"); 
        
        //Start Crowd parameters
        //The navmesh loader. 
        contCrowdParam.addChild(new Label("Crowd Parameters"));
        contCrowdParam.addChild(new Label("NavMesh"), "split 2"); 
        fieldNavMesh = contCrowdParam.addChild(new TextField("test.nm"), "growx");
        fieldNavMesh.setSingleLine(true);
        

        
        //Max CrowdAgents for the crowd.
        contCrowdParam.addChild(new Label("Max Agents"), "split 2, growx"); 
        doc = new DocumentModelFilter();
        doc.setInputTransform(TextFilters.numeric());
        doc.setText("100");
        fieldMaxAgents = contCrowdParam.addChild(new TextField(doc));
        fieldMaxAgents.setSingleLine(true);
        fieldMaxAgents.setPreferredWidth(50);
        
        //Max CrowdAgent radius for an agent in the crowd.
        contCrowdParam.addChild(new Label("Max Agent Radius"), "split 2, growx");
        fieldMaxAgentRadius = contCrowdParam.addChild(new TextField("0.6"));
        fieldMaxAgentRadius.setSingleLine(true);
        fieldMaxAgentRadius.setPreferredWidth(50);                    
  
        
        
        //Container for list movement.
        Container contListMoveType = new Container(new MigLayout("wrap", "[grow]"));
        contListMoveType.setName("CrowdBuilderState contListMoveType");
        contListMoveType.setAlpha(0, false);
        contCrowd.addChild(contListMoveType, "top, growx");
        
        //Movement types for the crowd.
        contListMoveType.addChild(new Label("Movement Type"));
        listBoxMoveType = contListMoveType.addChild(new ListBox<>(), "growx, growy");
        listBoxMoveType.setName("listBoxMoveType");
        listBoxMoveType.getSelectionModel().setSelection(0);
        listBoxMoveType.getModel().add("BCC");
        listBoxMoveType.getModel().add("DIRECT");
        listBoxMoveType.getModel().add("CUSTOM");
        listBoxMoveType.getModel().add("NONE");
        
        
        
        //Container that holds to Active Crowds.
        Container contActiveCrowds = new Container(new MigLayout("wrap", "[grow]"));
        contActiveCrowds.setName("CrowdBuilderState contActiveCrowds");
        contActiveCrowds.setAlpha(0, false);
        contCrowd.addChild(contActiveCrowds, "wrap, flowy, growx, growy");
        contActiveCrowds.addChild(new Label("Active Crowds"));
        listBoxActiveCrowds = contActiveCrowds.addChild(new ListBox<>(), "growx, growy"); 
        listBoxActiveCrowds.setCellRenderer(new DefaultCellRenderer<Crowd>(new ElementId("list.item"), null) {
            
            @Override
            protected String valueToString( Crowd crowd ) {
                String txt = "Crowd [ ";
                CrowdManager crowdManager = getState(CrowdManagerAppstate.class).getCrowdManager();
                int numberOfCrowds = crowdManager.getNumberOfCrowds();
                for (int i = 0; i < numberOfCrowds; i++) {
                    if (crowdManager.getCrowd(i).equals(crowd)) {
                        txt = txt + i + " ] Size [ ";
                        break;
                    }
                }
                
                txt = txt + crowd.getAgentCount() + " ]";
                
                return txt;
            }
        });
        
        listBoxActiveCrowds.setName("listBoxActiveCrowds");
        selectionRef = listBoxActiveCrowds.getSelectionModel().createReference();  
        modelRef = listBoxActiveCrowds.getModel().createReference();
        
        //Button to stop the Crowd.
        contActiveCrowds.addChild(new ActionButton(new CallMethodAction("Shutdown Crowd", this, "shutdown")), "top");
        
        
        
        //Label for the Obstacle Avoidance params. Shares same row as 
        //Active Grids label. Spans fist two columns. Give this its own row so 
        //can can align parameters with the listBox.
        Container contAvoidLabel = new Container(new MigLayout(null));
        contAvoidLabel.setName("CrowdBuilderState contAvoidLabel");
        contAvoidLabel.setAlpha(0, false);
        contAvoidLabel.addChild(new Label("Obstacle Avoidance Parameters")); 
        contCrowd.addChild(contAvoidLabel, "span 2");
        
        
        
        //Label for Active Grids listBox. Shares same row as 
        //Obstacle Avoidance Parameters label. 
        Container contActiveAgentsLabel = new Container(new MigLayout(null));
        contActiveAgentsLabel.setName("CrowdBuilderState contActiveAgentsLabel");
        contActiveAgentsLabel.setAlpha(0, false);
        contActiveAgentsLabel.addChild(new Label("Active Grids")); 
        contCrowd.addChild(contActiveAgentsLabel, "wrap");
        
        
        
        //Container that holds the obstacle avoidance parameters for CrowdAgents.
        Container contAvoidance = new Container(new MigLayout("wrap", "[grow]"));
        contAvoidance.setName("CrowdBuilderState contAvoidance");
        contAvoidance.setAlpha(0, false);
        contCrowd.addChild(contAvoidance, "growx, growy");
                
        //Velocity Bias.
        contAvoidance.addChild(new Label("velBias"), "split 2, growx"); 
        fieldVelocityBias = contAvoidance.addChild(new TextField("0.4"));
        fieldVelocityBias.setSingleLine(true);
        fieldVelocityBias.setPreferredWidth(50);
        
        //Weight desired velocity.
        contAvoidance.addChild(new Label("weightDesVel"), "split 2, growx"); 
        fieldWeightDesVel = contAvoidance.addChild(new TextField("2.0"));
        fieldWeightDesVel.setSingleLine(true);
        fieldWeightDesVel.setPreferredWidth(50);
        
        //Weight current velocity.
        contAvoidance.addChild(new Label("weightCurVel"), "split 2, growx"); 
        fieldWeightCurVel = contAvoidance.addChild(new TextField(".75"));
        fieldWeightCurVel.setSingleLine(true);
        fieldWeightCurVel.setPreferredWidth(50);
        
        //Weight side.
        contAvoidance.addChild(new Label("weightSide"), "split 2, growx"); 
        fieldWeightSide = contAvoidance.addChild(new TextField(".75"));
        fieldWeightSide.setSingleLine(true);
        fieldWeightSide.setPreferredWidth(50);        
        
        //Weight to impact.
        contAvoidance.addChild(new Label("weightToi"), "split 2, growx"); 
        fieldWeightToi = contAvoidance.addChild(new TextField("2.5"));
        fieldWeightToi.setSingleLine(true);
        fieldWeightToi.setPreferredWidth(50);  
        
        //Horrizon time.
        contAvoidance.addChild(new Label("horizTime"), "split 2, growx"); 
        fieldHorizTime = contAvoidance.addChild(new TextField("2.5"));
        fieldHorizTime.setSingleLine(true);
        fieldHorizTime.setPreferredWidth(50);                
                
        //Grid Size.
        contAvoidance.addChild(new Label("gridSize"), "split 2, growx"); 
        doc = new DocumentModelFilter();
        doc.setInputTransform(TextFilters.numeric());
        doc.setText("33");
        fieldGridSize = contAvoidance.addChild(new TextField(doc));
        fieldGridSize.setSingleLine(true);
        fieldGridSize.setPreferredWidth(50);
        
        //Adaptive Divisions.
        contAvoidance.addChild(new Label("adaptiveDivs"), "split 2, growx"); 
        doc = new DocumentModelFilter();
        doc.setInputTransform(TextFilters.numeric());
        doc.setText("7");
        fieldAdaptiveDivs = contAvoidance.addChild(new TextField(doc));
        fieldAdaptiveDivs.setSingleLine(true);
        fieldAdaptiveDivs.setPreferredWidth(50);
        
        //Adaptive Rings.
        contAvoidance.addChild(new Label("adaptiveRings"), "split 2, growx"); 
        doc = new DocumentModelFilter();
        doc.setInputTransform(TextFilters.numeric());
        doc.setText("2");
        fieldAdaptiveRings = contAvoidance.addChild(new TextField(doc));
        fieldAdaptiveRings.setSingleLine(true);
        fieldAdaptiveRings.setPreferredWidth(50);
        
        //Adaptive Depth.
        contAvoidance.addChild(new Label("adaptiveDepth"), "split 2, growx"); 
        doc = new DocumentModelFilter();
        doc.setInputTransform(TextFilters.numeric());
        doc.setText("5");
        fieldAdaptiveDepth = contAvoidance.addChild(new TextField(doc));
        fieldAdaptiveDepth.setSingleLine(true);
        fieldAdaptiveDepth.setPreferredWidth(50);        
        
        
        
        //Holds the listbox for avoidance parameters.
        Container contListBoxAvoidance = new Container(new MigLayout("wrap", "[grow]"));
        contListBoxAvoidance.setName("CrowdBuilderState contListBoxAvoidance");
        contListBoxAvoidance.setAlpha(0, false);
        contCrowd.addChild(contListBoxAvoidance, "growx, top");
        
        //Parameters list.
        listBoxAvoidance = contListBoxAvoidance.addChild(new ListBox<>(), "growx"); 
        listBoxAvoidance.setName("listBoxAvoidance");
        listBoxAvoidance.setVisibleItems(1);
        
        //Populate the list with ObstacleAvoidanceParams string. Thereafter the
        //update loop will take over.
        for (int i = 0; i < 8; i++) {
            String params = "<=====    " + i + "    =====>\n"
                    + defaultOAPText; 
                
            listBoxAvoidance.getModel().add(params);
        }
        listBoxAvoidance.getSelectionModel().setSelection(0);
        
        //Update a parameter button.
        contListBoxAvoidance.addChild(new ActionButton(new CallMethodAction("Update Parameter", this, "updateParam")));     
        
        
        
        //Container for Active Grids.
        Container contActiveGrid = new Container(new MigLayout("wrap", "[grow]"));
        contActiveGrid.setName("CrowdBuilderState contActiveGrid");
        contActiveGrid.setAlpha(0, false);
        contCrowd.addChild(contActiveGrid, "wrap, growx, top");
        
        //The Active Grids listBox.
        listBoxActiveGrids = contActiveGrid.addChild(new ListBox<>(), "growx");
        listBoxActiveGrids.setName("listBoxActiveGrid");
        listBoxActiveGrids.getModel().add(defaultActiveGridText);

        
        
        //Holds the Legend and Setup buttons.
        Container contButton = new Container(new MigLayout(null, // Layout Constraints
                "[]push[][]")); // Column constraints [min][pref][max]
        contButton.setName("CrowdBuilderState contButton");
        contButton.setAlpha(1, false);
        contCrowd.addChild(contButton, "growx, span 2"); //cell col row span w h
        //Help
        contButton.addChild(new ActionButton(new CallMethodAction("Help", this, "showHelp")));
        //Button to start the Crowd.
        contButton.addChild(new ActionButton(new CallMethodAction("Start Crowd", this, "startCrowd")));
        
        
        
        //Create the container that will hold the tab panel for BuildGridGui and 
        //BuildParamGui gui.
        contTabs = new Container(new MigLayout("wrap"));
        contTabs.setName("CrowdBuilderState contTabs");
        //Make it dragable.
        DragHandler dragHandler = new DragHandler();
        CursorEventControl.addListenersToSpatial(contTabs, dragHandler);
        contTabs.addChild(new Label("Crowd Builder"));
        
        
        
        //Create the tabbed panel.
        TabbedPanel tabPanel = contTabs.addChild(new TabbedPanel());
        
        
        
        //Modify tabs to stretch collapse button. MigLayout(null, "[grow, fill]"))
        //Add a rollup panel for the crowd settings panel.
        RollupPanel rollCrowd = new RollupPanel("Expand / Collapse", 
                contCrowd, "glass");
        rollCrowd.getTitleContainer().setLayout(new MigLayout(null, "[grow, fill]"));
        rollCrowd.getTitleElement().removeFromParent();
        rollCrowd.getTitleContainer().addChild(rollCrowd.getTitleElement());
        rollCrowd.getTitleElement().setTextHAlignment(HAlignment.Center);
        rollCrowd.setAlpha(0, false); 
        rollCrowd.setOpen(false);
        tabPanel.addTab("Crowd", rollCrowd);
        
        
        
        //Add a rollup panel so can hide agent grid.
        RollupPanel rollAgentGrid = new RollupPanel("Expand / Collapse", 
                getState(AgentGridState.class).getContAgentGrid(), "glass");
        rollAgentGrid.getTitleContainer().setLayout(new MigLayout(null, "[grow, fill]"));
        rollAgentGrid.getTitleElement().removeFromParent();
        rollAgentGrid.getTitleContainer().addChild(rollAgentGrid.getTitleElement());
        rollAgentGrid.getTitleElement().setTextHAlignment(HAlignment.Center);
        rollAgentGrid.setAlpha(0, false);
        rollAgentGrid.setOpen(false);
        tabPanel.addTab("Agent Grid", rollAgentGrid);
        
        
        
        //Add a rollup panel so can hide CrowdAgent parameters.
        RollupPanel rollAgentParam = new RollupPanel("Expand / Collapse", 
                getState(AgentParamState.class).getContAgentParams(), "glass");
        rollAgentParam.getTitleContainer().setLayout(new MigLayout(null, "[grow, fill]"));
        rollAgentParam.getTitleElement().removeFromParent();
        rollAgentParam.getTitleContainer().addChild(rollAgentParam.getTitleElement());
        rollAgentParam.getTitleElement().setTextHAlignment(HAlignment.Center);
        rollAgentParam.setAlpha(0, false); 
        rollAgentParam.setOpen(false);
        tabPanel.addTab("Agent Parameters", rollAgentParam);
        
        
        
        int height = getApplication().getCamera().getHeight();
        contTabs.setLocalTranslation(new Vector3f(0, height, 0));
        
        ((SimpleApplication) getApplication()).getGuiNode().attachChild(contTabs);
    }

    /**
     * Called by the DemoApplication F1 button ActionListener as part of a chain 
     * detachment of states. This is the start of the detachment chain.
     * CrowdBuilderState(onDisable)=>AgentParamState(onDisable)=>AgentGridState(onDisable)
     */
    @Override
    protected void onDisable() {
        if (getStateManager().hasState(getState(AgentParamState.class))) {
            getStateManager().getState(AgentParamState.class).setEnabled(false);
        }
        getStateManager().detach(this);
    }
    
    @Override
    public void update(float tpf) {

        //Have to check for both selectionRef(the selections themselves) and 
        //modelRef(the list of crowds) updates to fully know whether the 
        //reference has been updated.
        if( selectionRef.update() || modelRef.update()) {
            // Selection has changed and the model or selection is empty.
            if ( selectionRef.get().isEmpty() ||  modelRef.get().isEmpty()) {
                //Load defaults since there is no data to update.
                LOG.info("Update Loop empty reference - selectionRef [{}] modelRef [{}]", selectionRef.get().isEmpty(), modelRef.get().isEmpty());
                //The ObstacleAvoidanceParams string for the listbox.      
                for (int i = 0; i < 8; i++) {
                    String params = "<=====    " + i + "    =====>\n" + defaultOAPText; 
                    //Remove selected parameter from listBoxAvoidance. When 
                    //remove or insert is used on a Lemur ListBox, getSelection()
                    //will not be updated to anything other than the last item 
                    //in the ListBox. If the ListBox size is not affected
                    //by removal, such as following it with an insert, this can 
                    //be ignored. Otherwise it's best to setSelection(-1) and
                    //force the user to make a selection. If this changes, this 
                    //must be reworked to account for changes.
                    listBoxAvoidance.getModel().remove(i);
                    //Insert the new parameters into listBoxAvoidance.
                    listBoxAvoidance.getModel().add(i, params);
                }
                
                listBoxActiveGrids.getModel().clear();
                listBoxActiveGrids.getModel().add(defaultActiveGridText);
                
            } else {
                Crowd crowd = getSelectedCrowd();
                //Look for the selectedCrowd in mapCrowds rather than pulling 
                //directly from CrowdManager in case CrowdState is running
                //at same time as gui.
                if (crowd != null) {
                    for (int i = 0; i < 8; i++) {
                        ObstacleAvoidanceParams oap = crowd.getObstacleAvoidanceParams(i);
                        //Remove selected parameter. Lemur does not currently 
                        //update the selection when items are removed or 
                        //inserted. If this changes, this must be reworked to 
                        //account for changes.
                        listBoxAvoidance.getModel().remove(i);
                        //Insert the new parameters into the list by converting
                        //the oap to string.
                        listBoxAvoidance.getModel().add(i, formatOAP(oap, i));
                    }
                    
                    List<Grid> mapGrids = getState(AgentGridState.class).getMapGrids(); 
                    listBoxActiveGrids.getModel().clear();
                    for (Grid grid: mapGrids) {
                        //We only need one agent to know if the Crowd contains
                        //this grid, get first.
                        CrowdAgent gridAgent = grid.getListGridAgent().get(0).getCrowdAgent();
                        if (crowd.getActiveAgents().contains(gridAgent)) {
                            String txt = grid.getGridName() 
                                    + " < " + grid.getListGridAgent().size() 
                                    + "/" + crowd.getActiveAgents().size() 
                                    + "/" + crowd.getAgentCount() + " >";
                            listBoxActiveGrids.getModel().add(txt);
                        }
                    }
                    
                    if (listBoxActiveGrids.getModel().isEmpty()) {
                        listBoxActiveGrids.getModel().add(defaultActiveGridText);
                    }
                    
                } 
            }
        }

    }
    
    //Explains the Crowd parameters.
    private void showHelp() {
                
        String[] msg = { 
        "NavMesh - The navigation mesh to use for planning.",
        " ",
        "Max Agents - The maximum number of agents the crowd can manage. [Limit: >= 1]",
        "  ",
        "Max Agent Radius - The maximum radius of any agent that will be added to the crowd.",
        " ",
        "Movement Type - Each type determines how movement is applied to an agent.",
        " ",
        "* BCC - Use physics and the BetterCharacterControl to set move and view direction.",
        " ",
        "* DIRECT - Direct setting of the agents translation. No controls are needed for,",
        "movement but you must set the view direction yourself.",
        " ",
        "* CUSTOM - With custom, you implement the applyMovement() method from the",
        "ApplyFunction interface. This will give you access to the agents CrowdAgent object, the",
        "new position and the velocity of the agent.",
        " ",
        "* NONE - No movement is implemented. You can freely interact with the crowd and",
        "implement your own movement soloutions.",
        " ",
        "Active Crowds - The list of active crowds. The format for a crowd is the crowd number", 
        "and the active agents limit for the crowd. One crowd must be selected before any", 
        "agents can be added or targets for agents can be set.", 
        " ",
        "Obstacle Avoidance Parameters - The shared avoidance configuration for an Agent", 
        "inside the crowd. When first instantiating the crowd, you are allotted eight parameter", 
        "objects in total. All eight slots are filled with the defaults listed below.", 
        " ",
        "[Defaults]",
        "velBias             = 0.4f \t    horizTime         = 2.5f",
        "weightDesVel = 2.0f \t    gridSize             = 33",
        "weightCurVel  = 0.75f \t adaptiveDivs     = 7",
        "weightSide      = 0.75f \t adaptiveRings   = 2",
        "weightToi         = 2.5f \t   adaptiveDepth  = 5",
        " ",
        "You may not remove any default parameter from the Crowd, however, you can",
        "overwrite them. To change any parameter, select the crowd to be updated from the",
        "Active Crowds window, set the parameters you desire in the Obstacle Avoidance", 
        "Parameters section, then select any parameter from the list and click the",
        "[ Update Parameter ] button.",
        " ",
        "* velBias - The velocity bias describes how the sampling patterns is offset from the (0,0)", 
        "based on the desired velocity. This allows to tighten the sampling area and cull a lot of", 
        "samples. [Limit: 0-1]",
        " ",
        "* weightDesVel - How much deviation from desired velocity is penalized, the more", 
        "penalty applied to this, the more \"goal oriented\" the avoidance is, at the cost of getting", 
        "more easily stuck at local minima. [Limit: >= 0]",
        " ",
        "* weightCurVel - How much deviation from current velocity is penalized, the more", 
        "penalty applied to this, the more stubborn the agent is. This basically is a low pass filter,", 
        "and very important part of making things work.",
        " ",
        "* weightSide - In order to avoid reciprocal dance, the agents prefer to pass from right,", 
        "this weight applies penalty to velocities which try to take over from the wrong side.",
        " ",
        "* weightToi - How much penalty is added based on time to impact. Too much penalty", 
        "and the agents are shy, too little and they avoid too late.",
        " ",
        "* horizTime - Time horizon, this affects how early the agents start to avoid each other.", 
        "Too long horizon and the agents are scared of going through tight spots, and too small,", 
        "and they avoid too late (closely related to weightToi).",
        " ",
        "* gridSize - ???",
        " ",
        "* adaptiveDivs - Number of divisions per ring. [Limit: 1-32]",
        " ",
        "* adaptiveRings - Number of rings. [Limit: 1-4]",
        " ",
        "* adaptiveDepth - Number of iterations at best velocity.",
        " ",
        "Active Grids - Displays information about active grids residing in the selected crowd.", 
        "The format being <Grid Size/Crowd Active Agents Size/Crowd Active Agents Limit>."
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
     * Shuts down crowds listed in the Active Crowds list. When CrowdState is
     * enabled this will not be affected due to using the crowd of mapCrowds
     * to remove the Crowd from the CrowdManager. 
     */
    private void shutdown() {

        //Get the Crowd from selectedParam. we need this for accurate removal 
        //from listBoxActiveCrowds.
        Integer selection = listBoxActiveCrowds.getSelectionModel().getSelection();
        
        //Check to make sure the crowd has been selected.
        if (selection == null) {
            displayMessage("Select a crowd from the [ Active Crowds ] list.", 0);
            return;
        }
        
        //Get the crowds name from the listBoxActiveCrowds selectedParam.
        Crowd selectedCrowd = listBoxActiveCrowds.getModel().get(selection);

        //We check mapCrowds to see if the key exists. If not, go no further.
        if (!mapCrowds.containsKey(selectedCrowd)) {
            displayMessage("No crowd found by that name.", 0);
            return;
        }
        
        //We have a valid crowd so remove it from the map, the CrowdManager and 
        //the listBoxActiveCrowds.
        Iterator<Crowd> iterator = mapCrowds.keySet().iterator();        
        while (iterator.hasNext()) {
            Crowd crowd = iterator.next();
            if (crowd.equals(selectedCrowd)) {
                List<CrowdAgent> activeAgents = crowd.getActiveAgents();
                List<Grid> listGrids = getState(AgentGridState.class).getMapGrids();
                boolean found = false;
                for (CrowdAgent ca: activeAgents) {
                    for (Grid grid: listGrids) {
                        List<GridAgent> listGridAgent = grid.getListGridAgent();
                        for (GridAgent ga: listGridAgent) {
                            if (ga.getCrowdAgent().equals(ca)) {
                                //We have a CrowdAgent and a GridAgent so check 
                                //for Crowd specific control to manipulate or 
                                //remove.
                                
                                //Physics agents need to have their BCC reset.
                                if (ga.getSpatialForAgent().getControl(PhysicsAgentControl.class) != null) {
                                    LOG.info("Resetting Move [{}] idx [{}].", ga.getSpatialForAgent(), ga.getCrowdAgent().idx);
                                    ga.getSpatialForAgent().getControl(PhysicsAgentControl.class).stopFollowing();
                                }
                                
                                //DebugMoveControl is crowd specific so remove 
                                //if found.
                                if (ga.getSpatialForAgent().getControl(DebugMoveControl.class) != null) {
                                    LOG.info("Removing DebugMoveControl [{}] idx [{}].", ga.getSpatialForAgent(), ga.getCrowdAgent().idx);
                                    ga.getSpatialForAgent().removeControl(DebugMoveControl.class);
                                }
                                
                                //CrowdChangeControl is crowd specific so remove
                                //if found.
                                LOG.info("Removing CrowdChangeControl [{}] idx [{}].", ga.getSpatialForAgent(), ga.getCrowdAgent().idx);
                                ga.getSpatialForAgent().removeControl(CrowdChangeControl.class);
                                
                                //We have a GridAgent so notify outter loop it 
                                //was found and break out.
                                found = true;
                                break;
                            }
                        }
                        
                        //We found a GridAgent so this loop is done.
                        if (found) {
                           break; 
                        }
                    } 
                    //Remove CrowdAgent from crowd.
                    LOG.info("Removing idx [{}] crowd [{}]", ca.idx, crowd);
                    crowd.removeAgent(ca);
                }
                
                //To fully remove the crowd we have to remove it from the 
                //CrowdManager, mapCrowds (removes the query object also), and 
                //the listBoxActiveCrowds.
                getState(CrowdManagerAppstate.class).getCrowdManager().removeCrowd(crowd);
                listBoxActiveCrowds.getModel().remove((int)selection);
                //Lemur getSelected() does not update if you remove or insert 
                //into a listBox. Best to set it to -1 (unselected) and force
                //user to reselect.
                listBoxActiveCrowds.getSelectionModel().setSelection(-1);
                iterator.remove();
                break;
            }
        } 

    }
    
    /**
     * Starts and adds a crowd to the CrowdManager.
     */
    private void startCrowd() {
        int maxAgents;
        float maxAgentRadius;
        String mesh;

        //The name of the mesh to load.                
        if (fieldNavMesh.getText().isEmpty()) {
            displayMessage("You must enter a [ NavMesh ] name.", 0);
            return;
        } else {
            mesh = fieldNavMesh.getText();
        }
        
        //The max CrowdAgents for the crowd. Uses numeric doc filter to prevent bad data.
        if (fieldMaxAgents.getText().isEmpty()) {
            displayMessage("[ Max Agents ] requires a valid int value.", 0);
            return;
        } else {
            maxAgents = new Integer(fieldMaxAgents.getText());
            //Stop useless input.
            if (maxAgents < 1) {
                displayMessage("[ Max Agents ] requires a int value >= 1", 0);
                return;
            }
        }
        
        //The max CrowdAgent radius for an agent in the crowd.
        if (!getState(GuiUtilState.class).isNumeric(fieldMaxAgentRadius.getText()) 
        ||  fieldMaxAgentRadius.getText().isEmpty()) {
            displayMessage("[ Max Agent Radius ] requires a valid float value.", 0);
            return;
        } else {
            maxAgentRadius = new Float(fieldMaxAgentRadius.getText());
            //Stop negative input.
            if (maxAgentRadius <= 0.0f ) {
                displayMessage("[ Max Agent Radius ] requires a float value between > 0.", 0);
                return;
            }
        }
        
        MovementApplicationType applicationType = null;
        
        switch (listBoxMoveType.getSelectionModel().getSelection()) {
            case 0:
                applicationType = MovementApplicationType.BETTER_CHARACTER_CONTROL;
                break;
            case 1: 
                applicationType = MovementApplicationType.DIRECT;
                break;
            case 2:
                applicationType = MovementApplicationType.CUSTOM;
                break;
            case 3:
                applicationType = MovementApplicationType.NONE;
        }
        
        MeshSetReader msr = new MeshSetReader();
        try {
            //Read in the saved navMesh with same maxVertPerPoly(3) saved. Will 
            //be added to mapCrowds as a key using the text returned by 
            //fieldCrowdName.
            NavMesh navMesh = msr.read(new FileInputStream(mesh), 3);
            //Create the query object for pathfinding in this Crowd. Will be 
            //added to the mapCrowds as a crowd so each query object is referenced.  
            NavMeshQuery query = new NavMeshQuery(navMesh);
            
            //Create the crowd.
            Crowd crowd = new Crowd(applicationType, maxAgents, maxAgentRadius, navMesh);
            
            //Add to CrowdManager, mapCrowds, and listBoxActiveCrowds.
            getState(CrowdManagerAppstate.class).getCrowdManager().addCrowd(crowd);
            mapCrowds.put(crowd, query);
            listBoxActiveCrowds.getModel().add(crowd); 
        } catch (IOException | NoSuchFieldException | IllegalAccessException ex) {
            LOG.error("{} {}", CrowdBuilderState.class.getName(), ex);
        }

    }
    
    /**
     * Takes a string and looks for the token '$' to format a new string based 
     * on the char following the token. Builds the new ObstacleAvoidanceParams 
     * object for the selected Crowd and inserts it into the appropriate slot. 
     * It is expected the Crowd is running.
     * 
     * @param format The string to be formatted.
     */
    private void updateParam() {
        float velBias;
        float weightDesVel;
        float weightCurVel;
        float weightSide;
        float weightToi;
        float horizTime;
        int gridSize;
        int adaptiveDivs;
        int adaptiveDepth;
        int adaptiveRings;
        Integer selectedParam;
                
        //The velocity bias settings.
        if (!getState(GuiUtilState.class).isNumeric(fieldVelocityBias.getText()) 
        ||  fieldVelocityBias.getText().isEmpty()) {
            displayMessage("[ velocityBias ] requires a valid float value.", 0);
            return;
        } else {
            velBias = new Float(fieldVelocityBias.getText());
            //Stop negative input.
            if (velBias < 0.0f || velBias > 1) {
                displayMessage("[ fieldVelocityBias ] requires a float value between 0 and 1 inclusive.", 0);
                return;
            }
        }
        
        //The weighted desired velocity settings.
        if (!getState(GuiUtilState.class).isNumeric(fieldWeightDesVel.getText()) 
        ||  fieldWeightDesVel.getText().isEmpty()) {
            displayMessage("[ weightDesVel ] requires a valid float value.", 0);
            return;
        } else {
            weightDesVel = new Float(fieldWeightDesVel.getText());
            //Stop negative input.
            if (weightDesVel < 0.0f) {
                displayMessage("[ weightDesVel ] requires a float value >= 0.", 0);
                return;
            }
        }

        //The weighted current velocity settings.
        if (!getState(GuiUtilState.class).isNumeric(fieldWeightCurVel.getText()) 
        ||  fieldWeightCurVel.getText().isEmpty()) {
            displayMessage("[ fieldWeightCurVel ] requires a valid float value.", 0);
            return;
        } else {
            weightCurVel = new Float(fieldWeightCurVel.getText());
            //Stop negative input.
            if (weightCurVel < 0.0f) {
                displayMessage("[ fieldWeightCurVel ] requires a float value >= 0.", 0);
                return;
            }
        }
        
        //The weighted side settings.
        if (!getState(GuiUtilState.class).isNumeric(fieldWeightSide.getText()) 
        ||  fieldWeightSide.getText().isEmpty()) {
            displayMessage("[ fieldWeightSide ] requires a valid float value.", 0);
            return;
        } else {
            weightSide = new Float(fieldWeightSide.getText());
            //Stop negative input.
            if (weightSide < 0.0f) {
                displayMessage("[ fieldWeightSide ] requires a float value >= 0.", 0);
                return;
            }
        }
        
        //The weight to impact settings.
        if (!getState(GuiUtilState.class).isNumeric(fieldWeightToi.getText()) 
        ||  fieldWeightToi.getText().isEmpty()) {
            displayMessage("[ fieldWeightToi ] requires a valid float value.", 0);
            return;
        } else {
            weightToi = new Float(fieldWeightToi.getText());
            //Stop negative input.
            if (weightToi < 0.0f) {
                displayMessage("[ fieldWeightToi ] requires a float value >= 0.", 0);
                return;
            }
        }        
                
        //The horizon settings.
        if (!getState(GuiUtilState.class).isNumeric(fieldHorizTime.getText()) 
        ||  fieldHorizTime.getText().isEmpty()) {
            displayMessage("[ fieldHorizTime ] requires a valid float value.", 0);
            return;
        } else {
            horizTime = new Float(fieldHorizTime.getText());
            //Stop negative input.
            if (weightToi < 0.0f) {
                displayMessage("[ fieldHorizTime ] requires a float value >= 0.", 0);
                return;
            }
        }       
        
        //The grid size settings. Uses numeric doc filter to prevent bad data.
        if (fieldGridSize.getText().isEmpty()) {
            displayMessage("[ fieldGridSize ] requires a valid int value.", 0);
            return;
        } else {
            gridSize = new Integer(fieldGridSize.getText());
            //Stop useless input.
            if (gridSize < 1) {
                displayMessage("[ fieldGridSize ] requires a int value >= 1.", 0);
                return;
            }
        }
        
        //The adaptive divisions settings. Uses numeric doc filter to prevent
        //bad data.
        if (fieldAdaptiveDivs.getText().isEmpty()) {
            displayMessage("[ adaptiveDivs ] requires a valid int value.", 0);
            return;
        } else {
            adaptiveDivs = new Integer(fieldAdaptiveDivs.getText());
            //Stop useless input.
            if (adaptiveDivs < 1 || adaptiveDivs > 32) {
                displayMessage("[ adaptiveDivs ] requires a int value between 1 and 32 inclusive.", 0);
                return;
            }
        }
        
         //The adaptive depth settings. Uses numeric doc filter to prevent bad data.
        if (fieldAdaptiveDepth.getText().isEmpty()) {
            displayMessage("[ adaptiveDepth ] requires a valid int value.", 0);
            return;
        } else {
            adaptiveDepth = new Integer(fieldAdaptiveDepth.getText());
            //Stop useless input.
            if (adaptiveDepth < 1 ) {
                displayMessage("[ adaptiveDepth ] requires a int value >= 1.", 0);
                return;
            }
        }
        
        //The adaptive depth settings. Uses numeric doc filter to prevent bad data.
        if (fieldAdaptiveRings.getText().isEmpty()) {
            displayMessage("[ adaptiveRings ] requires a valid int value.", 0);
            return;
        } else {
            adaptiveRings = new Integer(fieldAdaptiveRings.getText());
            //Stop negative input.
            if (adaptiveRings < 1 || adaptiveRings > 4) {
                displayMessage("[ adaptiveRings ] requires a int value between 1 and 4 inclusive.", 0);
                return;
            }
        }
        
        //Get the selectedParam from listBoxAvoidance.
        selectedParam = listBoxAvoidance.getSelectionModel().getSelection();
        
        //Check to make sure a an avoidance parmeter has been selected.
        if (selectedParam == null) {
            displayMessage("You must select a [ Parameter ] from the list before it can be updated.", 0);
            return;
        }

        ObstacleAvoidanceParams params  = new ObstacleAvoidanceParams();
        params.velBias          = velBias;
        params.weightDesVel     = weightDesVel;
        params.weightCurVel     = weightCurVel;
        params.weightSide       = weightSide;
        params.weightToi        = weightToi;
        params.horizTime        = horizTime;
        params.gridSize         = gridSize;
        params.adaptiveDivs     = adaptiveDivs;
        params.adaptiveDepth    = adaptiveDepth;
        params.adaptiveRings    = adaptiveRings;

        //Inject the new parameter into the crowd. Check for the crowd in 
        //mapcrowds and if exists, update OAP params. Pulls the crowd reference 
        //from mapCrowds rather than the CrowdManager in case CrowdState is 
        //running. This will keep our crowd lookups in sync.
        if (getSelectedCrowd() != null) {
            getSelectedCrowd().setObstacleAvoidanceParams(selectedParam, params);
            //Remove selected parameter from listBoxAvoidance. Lemur does not 
            //currently update the selection when items are removed or inserted.
            //If this changes, this must be reworked to account for changes.
            listBoxAvoidance.getModel().remove((int)selectedParam);
            //Insert the new parameters into listBoxAvoidance.
            listBoxAvoidance.getModel().add(selectedParam, formatOAP(params, selectedParam));
        } else {
            LOG.error("Failed to find the selected crowd in mapCrowds [{}]", getSelectedCrowd());
            displayMessage("You must select a [ Active Crowd ] " 
                    + "from the list before a parameter can be updated.", 0);
        }
        
    }
    
    /**
     * Formats a Obstacle Avoidance Parameter into string form for insertion into  
     * a listBoxAvoidance selection.
     * Parameters are case sensitive and are as follows:
     *          'b' sets the Velocity Bias
     *          'd' sets the Adaptive Divisions
     *          'D' sets the Adaptive Depth
     *          'g' sets the Grid Size
     *          'h' sets the Horizon Time
     *          'i' sets the Weight To Impact
     *          'r' sets the Adaptive Rings
     *          's' sets the Weight Side
     *          'v' sets the Weight Desired Velocity
     *          'V' sets the Weight Current Velocity
     * 
     * @param oap The Obstacle Avoidance Parameter that needs string formating.
     * @param idx The index of the listboxAvoidance parameter.
     * @return The formated string.
     */
    private String formatOAP(ObstacleAvoidanceParams oap, int idx) {
        //If run into threading problems use StringBuffer.
        StringBuilder buf               = new StringBuilder();
        String i                        = null;
        String str = 
          "<=====    " + idx + "    =====>\n"  
        + "velBias              = $b\n"
        + "weightDesVel  = $v\n"
        + "weightCurVel   = $V\n"
        + "weightSide       = $s\n"
        + "weightToi         = $i\n"
        + "horizTime         = $h\n"
        + "gridSize            = $g\n"
        + "adaptiveDivs    = $d\n"               
        + "adaptiveRings  = $r\n"
        + "adaptiveDepth = $D";
        LOG.info("<========== BEGIN CrowdBuilderState oapToString [{}]==========>", idx);
        for ( int j = 0; j < str.length(); j++ ) {
            if ( str.charAt(j) != '$' ) {
                buf.append(str.charAt(j));
                continue;
            }
            
            char charAt = str.charAt(++j);
            
            switch (charAt) {
                case 'b': //Velocity Bias
                    i = "" + oap.velBias;
                    LOG.info("velBias       [{}]", oap.velBias);
                    break;
                case 'd': //Adaptive Divisions
                    i = "" + oap.adaptiveDivs;
                    LOG.info("adaptiveDivs  [{}]", oap.adaptiveDivs);
                    break;
                case 'D': //Adaptive Depth
                    i = "" + oap.adaptiveDepth;
                    LOG.info("adaptiveDepth [{}]", oap.adaptiveDepth);
                    break;
                case 'g': //Grid Size
                    i = "" + oap.gridSize;
                    LOG.info("gridSize      [{}]", oap.gridSize);
                    break;
                case 'h': //Horizon Time
                    i = "" + oap.horizTime;
                    LOG.info("horizTime     [{}]", oap.horizTime);
                    break;
                case 'i': //Weight To Impact
                    i = "" + oap.weightToi;
                    LOG.info("weightToi     [{}]", oap.weightToi);
                    break;
                case 'r': //Adaptive Rings
                    i = "" + oap.adaptiveRings;
                    LOG.info("adaptiveRings [{}]", oap.adaptiveRings);
                    break;
                case 's': //Weight Side
                    i = "" + oap.weightSide;
                    LOG.info("weightSide    [{}]", oap.weightSide);
                    break;
                case 'v': //Weight Desired Velocity
                    i = "" + oap.weightDesVel;
                    LOG.info("weightDesVel  [{}]", oap.weightDesVel);
                    break;
                case 'V': //Weight Current Velocity
                    i = "" + oap.weightCurVel;
                    LOG.info("weightCurVel  [{}]", oap.weightCurVel);
                    break;
            }
            buf.append(i);
        } 
        LOG.info("<========== END CrowdBuilderState oapToString [{}] ==========>", idx);
        return buf.toString();
        
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
     * Gets the query object for any selected Crowd.
     * 
     * @return The query object for a selected Crowd or null if the Crowd has 
     * not been selected in the Active Crowds list or if the query object 
     * doesn't exist in the mapCrowds list.
     */
    public NavMeshQuery getQuery() {
        
        Crowd crowd = getSelectedCrowd();
        
        //Check to make sure a crowd has been selected.
        if (crowd == null || !mapCrowds.containsKey(crowd)) {
            return null;
        } 
        
        return mapCrowds.get(crowd);
    }    
    
    /**
     * Gets the currently selected crowd from the Active Crowds list as a Crowd 
     * object.
     * 
     * @return The Crowd for the selection.
     */    
    public Crowd getSelectedCrowd() {
        //Get the crowd from listBoxActiveCrowds.
        Integer selectedCrowd = listBoxActiveCrowds.getSelectionModel().getSelection();

        //Check to make sure a crowd has been selected.
        if (selectedCrowd == null) {
            return null;
        }
        
        return listBoxActiveCrowds.getModel().get(selectedCrowd);
    }
    
    /**
     * Gets a crowd number from the CrowdManager by checking for the crowd 
     * object. Not really useful for anything other than logging.
     * 
     * @param crowd The crowd to lookup.
     * @return The crowd number for the given crowd.
     */
    public int getCrowdNumber(Crowd crowd) {
        CrowdManager crowdManager = getState(CrowdManagerAppstate.class).getCrowdManager();
        int numberOfCrowds = crowdManager.getNumberOfCrowds();
        for (int i = 0; i < numberOfCrowds; i++) {
            if (crowdManager.getCrowd(i).equals(crowd)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * @return the listBoxActiveCrowds
     */
    public ListBox<Crowd> getListBoxActiveCrowds() {
        return listBoxActiveCrowds;
    }
    
}
