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
import com.jme3.input.event.MouseButtonEvent;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.recast4j.demo.controls.CrowdAgentControl;
import com.jme3.recast4j.demo.controls.NavMeshChaserControl;
import com.jme3.recast4j.demo.layout.MigLayout;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.simsilica.lemur.ActionButton;
import com.simsilica.lemur.Button;
import com.simsilica.lemur.CallMethodAction;
import com.simsilica.lemur.Checkbox;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.GuiGlobals;
import com.simsilica.lemur.Label;
import com.simsilica.lemur.ListBox;
import com.simsilica.lemur.Panel;
import com.simsilica.lemur.TextField;
import com.simsilica.lemur.component.TbtQuadBackgroundComponent;
import com.simsilica.lemur.event.ConsumingMouseListener;
import com.simsilica.lemur.event.CursorEventControl;
import com.simsilica.lemur.event.DefaultMouseListener;
import com.simsilica.lemur.event.DragHandler;
import com.simsilica.lemur.event.MouseEventControl;
import com.simsilica.lemur.style.Attributes;
import com.simsilica.lemur.style.BaseStyles;
import com.simsilica.lemur.style.Styles;
import com.simsilica.lemur.text.DocumentModelFilter;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.List;
import org.recast4j.detour.crowd.CrowdAgentParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Robert
 */
public class TestGenState extends BaseAppState {

    private static final Logger LOG = LoggerFactory.getLogger(TestGenState.class.getName());

    private TextField fieldDistance;
    private DocumentModelFilter doc;
    private ListBox listBoxAvoidance;
    private ListBox listBoxAgent;
    private ListBox listBoxSize;
    private TextField fieldPosX;
    private TextField fieldPosY;
    private TextField fieldPosZ;
    private TextField fieldTestName;
    private Container contCrowd;
    private Checkbox checkTurns;
    private Checkbox checkAvoid;
    private Checkbox checkTopo;
    private Checkbox checkVis;
    private Checkbox checkSep;
    private List<Node> listAgents;
    private boolean loadAgents;
    
    @Override
    protected void initialize(Application app) {
        //Initialize the character list for the test. 
        listAgents = new ArrayList<>();
        loadAgents = false;
        
        GuiGlobals.initialize(app);
        BaseStyles.loadGlassStyle();
        GuiGlobals.getInstance().getStyles().setDefaultStyle("glass");
        Styles styles = GuiGlobals.getInstance().getStyles();
        Attributes attrs = styles.getSelector(Container.ELEMENT_ID, "glass");
        TbtQuadBackgroundComponent bg = attrs.get("background");
        bg.setColor(new ColorRGBA(0.25f, 0.5f, 0.5f, 1.0f));
        
        //Container for crowd configuration.
        contCrowd = new Container(new MigLayout("wrap 3"));
        contCrowd.setName("TestGenState crowdCont");
        contCrowd.addChild(new Label("Crowd Agent Generator"), "span, align 50%");
        //Make dragable.
        DragHandler dragHandler = new DragHandler();
        CursorEventControl.addListenersToSpatial(contCrowd, dragHandler);
       
        // First row.
        Container contRow1 = new Container(new MigLayout("wrap 5"));
        contRow1.setName("TestGenState rowOneCont");
        contRow1.addChild(new Label("Test Name"));
        
        doc = new DocumentModelFilter();
        //placeholder for filtering
        fieldTestName = contRow1.addChild(new TextField(doc));
        fieldTestName.setSingleLine(true);
        fieldTestName.setPreferredWidth(100);
        
        contRow1.addChild(new Label("Crowd Separation"));
        doc = new DocumentModelFilter();
        //placeholder for filtering        
        fieldDistance = contRow1.addChild(new TextField(doc));
        fieldDistance.setSingleLine(true);
        fieldDistance.setPreferredWidth(50);
        fieldDistance.setText("1.0");
        contRow1.addChild(new Label(".f"));
        contCrowd.addChild(contRow1, "span, align 50%");
           
        //Second row
        //Add model name here. Add the asset path butSetup #Model Section
        Container cont1Row2 = new Container(new MigLayout("wrap"));
        cont1Row2.setName("TestGenState rowTwoCont1");
        cont1Row2.addChild(new Label("Agent Type"));
        listBoxAgent = cont1Row2.addChild(new ListBox(), "align 50%");
        MouseEventControl.addListenersToSpatial(cont1Row2, ConsumingMouseListener.INSTANCE);
        listBoxAgent.getModel().add("Jamie");
        listBoxAgent.getSelectionModel().setSelection(0);
        contCrowd.addChild(cont1Row2);
        
        Container cont2Row2 = new Container(new MigLayout("wrap"));
        cont2Row2.setName("TestGenState rowTwoCont2");
        cont2Row2.addChild(new Label("Crowd Size"));
        listBoxSize = cont2Row2.addChild(new ListBox(), "align 50%");
        MouseEventControl.addListenersToSpatial(cont2Row2, ConsumingMouseListener.INSTANCE);
        int size = 1;
        for (int i = 0; i < 15; i++) {
            listBoxSize.getModel().add("" + size);
            size++;
        }
        listBoxSize.getSelectionModel().setSelection(0);
        contCrowd.addChild(cont2Row2);
        
        Container cont3Row2 = new Container(new MigLayout("wrap"));
        cont3Row2.setName("TestGenState rowTwoCont3");
        cont3Row2.addChild(new Label("Obstacle Avoidance Type"));
        listBoxAvoidance = cont3Row2.addChild(new ListBox(), "align 50%");
        MouseEventControl.addListenersToSpatial(cont3Row2, ConsumingMouseListener.INSTANCE);
        
        //Have to set this here since Crowd has package-private access the to 
        //the DT_CROWD_MAX_OBSTAVOIDANCE_PARAMS variable. Currently max is 8.
        for (int i = 0; i < 8; i++) {
            listBoxAvoidance.getModel().add(i);
        }
        listBoxAvoidance.getSelectionModel().setSelection(0);
        contCrowd.addChild(cont3Row2);
        
        //Third row  
        //TODO add a multi selection mode and move this to a ListBox. Change the
        //butSetup mega if/else check when done.
        Container contRow3 = new Container(new MigLayout("wrap 2"));
        contRow3.setName("TestGenState rowThreeCont");
        contRow3.addChild(new Label("Update Flags"), "span");
        checkTurns = contRow3.addChild(new Checkbox("ANTICIPATE_TURNS"));
        checkAvoid = contRow3.addChild(new Checkbox("OBSTACLE_AVOIDANCE"));
        checkTopo = contRow3.addChild(new Checkbox("OPTIMIZE_TOPO"));
        checkVis = contRow3.addChild(new Checkbox("OPTIMIZE_VIS"));
        checkSep = contRow3.addChild(new Checkbox("SEPARATION"));
        contCrowd.addChild(contRow3, "span, align 50%");
        
        //Fourth row
        Container contRow4 = new Container(new MigLayout("wrap 7"));
        contRow4.setName("TestGenState rowFourCont");
        contRow4.addChild(new Label("Start Position")); 
        //X
        contRow4.addChild(new Label("X:"));
        doc = new DocumentModelFilter();
        //placeholder for filtering
        fieldPosX = contRow4.addChild(new TextField(doc));
        fieldPosX.setSingleLine(true);
        fieldPosX.setPreferredWidth(50);
        fieldPosX.setText("0.0");
        
        //Y
        contRow4.addChild(new Label("Y:"));
        doc = new DocumentModelFilter();
        //placeholder for filtering
        fieldPosY = contRow4.addChild(new TextField(doc));
        fieldPosY.setSingleLine(true);
        fieldPosY.setPreferredWidth(50);
        fieldPosY.setText("0.0");
        
        //Z
        contRow4.addChild(new Label("Z:"));
        doc = new DocumentModelFilter();
        //placeholder for filtering        
        fieldPosZ = contRow4.addChild(new TextField(doc));
        fieldPosZ.setSingleLine(true);
        fieldPosZ.setPreferredWidth(50);
        fieldPosZ.setText("0.0");
        contCrowd.addChild(contRow4, "span, align 50%");
        
        //Fifth row
        Container contRow5 = new Container(new MigLayout("wrap 2"));
        contRow5.setName("TestGenState rowFiveCont");
        Button butSetup = contRow5.addChild(new Button("Generate Agents"));
        MouseEventControl.addListenersToSpatial(butSetup, new DefaultMouseListener() {
            private String testName;
            private float distance = -1;
            private String agent;
            private int flags;
            Vector3f startPos = new Vector3f(0, 0, 0);
            
            @Override
            protected void click( MouseButtonEvent event, Spatial target, Spatial capture ) {
                
                //Garbage in?, no test.
                if (!isNumeric(fieldDistance.getText()) 
                ||  fieldDistance.getText().isEmpty()) {
                    GuiGlobals.getInstance().getPopupState().showModalPopup(showPopup("Crowd Separation requires a valid float value."));
                    return;
                } else {
                    distance = new Float(fieldDistance.getText());
                    //Stop negative distance input for grid.
                    if (distance < 0.1f) {
                        GuiGlobals.getInstance().getPopupState().showModalPopup(showPopup("Crowd Separation requires a float value >= 0.1."));
                        return;
                    }
                }
                
                //###### Model Section #####
                //Model to use for the test. Selection is set to 0 when creating 
                //the listBoxAgent so shouldn't need to check for null.
                switch(listBoxAgent.getSelectionModel().getSelection()) {
                    case 0: 
                        agent = "Models/Jaime/Jaime.j3o";
                        break;
                    default: agent = null;
                }
                
                //Number of agents to place into the test in rows and columns.
                //Selection is set to 0 when creating the listBoxSize so 
                //shouldn't need to check for null.
                int size = listBoxSize.getSelectionModel().getSelection() + 1;
                
                //Set the CrowdAgentParams. Ugly but, lemurs ListBox doesn't 
                //have a muli selection model for so.
                if (!checkTurns.isChecked() 
                &&  !checkAvoid.isChecked() 
                &&  !checkTopo.isChecked() 
                &&  !checkVis.isChecked() 
                &&  !checkSep.isChecked()) {
                    GuiGlobals.getInstance().getPopupState().showModalPopup(showPopup("Select at least one Update Flag."));
                    return;
                } else {
                    flags = 0;
                    if (checkTurns.isChecked()) {
                        flags += CrowdAgentParams.DT_CROWD_ANTICIPATE_TURNS;
                    }
                    
                    if (checkAvoid.isChecked()) {
                        flags += CrowdAgentParams.DT_CROWD_OBSTACLE_AVOIDANCE;
                    }
                    
                    if (checkTopo.isChecked()) {
                        flags += CrowdAgentParams.DT_CROWD_OPTIMIZE_TOPO;
                    }
                    
                    if (checkVis.isChecked()) {
                        flags += CrowdAgentParams.DT_CROWD_OPTIMIZE_VIS;
                    }
                    
                    if (checkSep.isChecked()) {
                        flags += CrowdAgentParams.DT_CROWD_SEPARATION;
                    }
                }
                
                //Obstacle Avoidance Type. Selection is set to 0 when creating 
                //the listBoxAvoidance so shouldn't need to check for null. 
                int obstacleAvoidanceType = listBoxAvoidance.getSelectionModel().getSelection();
                
                //The starting position of the test. Sanity check.
                if (!isNumeric(fieldPosX.getText()) || fieldPosX.getText().isEmpty() 
                ||  !isNumeric(fieldPosY.getText()) || fieldPosY.getText().isEmpty() 
                ||  !isNumeric(fieldPosZ.getText()) || fieldPosZ.getText().isEmpty()) {
                    GuiGlobals.getInstance().getPopupState().showModalPopup(showPopup("Start Position requires a valid float value."));
                } else {
                    Float x = new Float(fieldPosX.getText());
                    Float y = new Float(fieldPosY.getText());
                    Float z = new Float(fieldPosZ.getText());
                    startPos = new Vector3f(x, y, z);
                }
                
                //The name of this test.
                if (fieldTestName.getText().isEmpty()) {
                    GuiGlobals.getInstance().getPopupState().showModalPopup(showPopup("You must enter a test name."));
                    return;
                } else{
                    testName = fieldTestName.getText();
                }
                 
                setupTest(agent, size, distance, flags, obstacleAvoidanceType, startPos, testName);
            }

            //Validate user input for float fields.
            private boolean isNumeric(String str) {
                NumberFormat formatter = NumberFormat.getInstance();
                ParsePosition pos = new ParsePosition(0);
                formatter.parse(str, pos);
                return str.length() == pos.getIndex();
            }
        });
        
        contRow5.addChild(new ActionButton(new CallMethodAction("Legend", this, "showLegend")));
        contCrowd.addChild(contRow5, "span, align 50%");
        
        centerComp(contCrowd);
    }

    @Override
    protected void cleanup(Application app) {
        //TODO: clean up what you initialized in the initialize method,
        //e.g. remove all spatials from rootNode
    }

    @Override
    protected void onEnable() {
//        ((SimpleApplication) getApplication()).setDisplayFps(false);
//        ((SimpleApplication) getApplication()).setDisplayStatView(false);
        ((SimpleApplication) getApplication()).getGuiNode().attachChild(contCrowd);
    }

    @Override
    protected void onDisable() {
//        ((SimpleApplication) getApplication()).setDisplayFps(true);
//        ((SimpleApplication) getApplication()).setDisplayStatView(true);
        contCrowd.removeFromParent();
    }
    
    @Override
    public void update(float tpf) {
        if (loadAgents) {
            for (Node agent: listAgents) {
                getStateManager().getState(BulletAppState.class).getPhysicsSpace().add(agent);
                ((SimpleApplication) getApplication()).getRootNode().attachChild(agent);
            }
            loadAgents = false;
        }
    }
    
    //Centers any lemur component to the middle of the screen.
    private void centerComp(Panel cont) {
        // Position the panel                                                            
        cont.setLocalTranslation((getApplication().getCamera().getWidth() - cont.getPreferredSize().x)/2, 
                (getApplication().getCamera().getHeight() + cont.getPreferredSize().y)/2, 0);
    }
    
    //Set the test parameters for this test.
    private void setupTest(String character, int size, float distance, int updateFlags, 
            int obstacleAvoidanceType, Vector3f startPos, String testName) {
        LOG.info("setupTest(agent [{}], size [{}], distance [{}], updateFlags[{}], "
        + "obstacleAvoidanceType [{}], startPos {}, testName [{}])", 
        character, size, distance, updateFlags, obstacleAvoidanceType, startPos, testName);
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                Node agent = (Node) getApplication().getAssetManager().loadModel(character);
                
                agent.setName(testName + "_r" + i + "_c"+ j);
                
                //Set Crowd parameters for the agent
                CrowdAgentControl cac = new CrowdAgentControl();
                cac.setFlags(updateFlags);
                cac.setObstacleAvoidanceType(obstacleAvoidanceType);
                
                //Set the start position for each spatial
                float startX = startPos.getX() + i * distance;
                float startY = startPos.getY();
                float startZ = startPos.getZ() + j * distance;
                Vector3f start = new Vector3f(startX, startY, startZ);
                agent.setLocalTranslation(start);
                Vector3f localTranslation = agent.getLocalTranslation();
                
                LOG.info("agent [{}] loc {}", agent.getName(), localTranslation);
                
                //The auto generation is based off model bounds and assumes a 
                //bipedal character in a T-Pose position. The general assumption 
                //being we want to set the collision shape of BCC to be around 
                //shoulder width and as near to the actual head height as 
                //possible. This would typically mean the largest number in the 
                //x or z direction would be the arm spread.
                //If this doesn't work we may have to add another parameter to 
                //the method to get the detail desired.
                BoundingBox bounds = (BoundingBox) agent.getWorldBound();
                float x = bounds.getXExtent();
                float y = bounds.getYExtent();
                float z = bounds.getZExtent();
                float xz = x < z ? x:z;
                
                LOG.info("X [{}] Z [{}] XZ [{}] /2 [{}]", x, z, xz, xz/2);
                
                agent.addControl(new BetterCharacterControl(xz/2, y*2, 20f));
                agent.addControl(new NavMeshChaserControl());
                
                //Add to agents list.
                listAgents.add(agent);
            }
        }
        loadAgents = true;
    }
    
    private void showLegend() {
        String msg = 
                  "Test Name = name used for the test. The agents used in "
                + "the test will all be named according to their insertion point "
                + "into the grid of agents. This is a required setting.\n\n"
                
                + "Example: In a size 3 grid (3x3 = 9 agents), the naming is as "
                + "follows where r = row, c = column, \n\n"
                + "First Row  = testName_r0_c0, testName_r0_c1, testName_r0_c2\n"
                + "Second Row = testname_r1_c0, testName_r1_c1, testName_r1_c2\n"
                + "Third Row  = testname_r2_c0, testName_r2_c1, testName_r2_c2\n\n" 
                
                + "Crowd Separation = spacing between agents in the grid. Must "
                + "be a setting of 0.1f or larger.\n\n"
                
                + "Agent Type = model to use for the test.\n\n"
                
                + "Crowd Size = number of agents to place into the test in rows "
                + "and columns. Limit = 15 which is 225 agents.\n\n"
                
                + "Example: 2 = 2 rows, 2 columns = a grid of 4 agents\n\n"
                
                + "Obstacle Avoidance Type = This is the ObstacleAvoidanceParams "
                + "number you setup when instantiating the crowd. "
                + "Currently, the max number of avoidance parameters that can be "
                + "set for the Crowd is eight.\n\n"
                
                + "Update Flags = Crowd agent update flags. This is a required "
                + "setting.\n\n"
                + "DT_CROWD_ANTICIPATE_TURNS, DT_CROWD_OBSTACLE_AVOIDANCE, "
                + "DT_CROWD_SEPARATION, DT_CROWD_OPTIMIZE_VIS, "
                + "DT_CROWD_OPTIMIZE_TOPO\n\n"
                
                + "Start Position = starting position the agents will spread out "
                + "evenly from to form the grid. "
                + "This is only used to generate the agents for the test so you "
                + "can drop your agents from above the navMesh id you wish. "
                + "The actual starting point of the agent is determined by its "
                + "final position on the navMesh.";
        
        Container contLegend = new Container(new MigLayout("wrap"));
        Label label = contLegend.addChild(new Label(msg));
        label.setMaxWidth(400);
        contLegend.addChild(new ActionButton(new CallMethodAction("Close", contLegend, "removeFromParent")));
        centerComp(contLegend);
        GuiGlobals.getInstance().getPopupState().showPopup(contLegend);
    }
    
    private Panel showPopup( String msg ) {
        Container window = new Container(new MigLayout("wrap"));
        window.addChild(new Label(msg));
        window.addChild(new ActionButton(new CallMethodAction("Close", window, "removeFromParent")), "align 50%");
        centerComp(window);
        return window;                                                              
    }
}
