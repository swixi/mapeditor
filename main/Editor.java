package main;

import datastructures.*;
import objectbrowser.*;
import tools.*;

import java.io.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.filechooser.*;
import java.net.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
//import org.apache.commons.net.ftp.*;
//import com.enterprisedt.net.ftp.*;

/*
 * The primary program used for creating, editing, saving, and loading maps for QuarantineOnline.
 * Editor allows access to an FTP server which stores all of the images for QuarantineOnline. It can save the images to the user's local disk for easier loading.
 * Editor can also access an SQL database to write a user's map to, for the game client to load from.
 */
public class Editor extends JApplet implements ActionListener
{
    //Constants
    protected final String HOST_NAME = "***REMOVED***";
    private static final String MAP_FILE_EXTENSION = ".qom"; //Quarantine Online Map
    private static final String VERSION_STRING = "Version 0.12";
    private static final String LAST_UPDATED_STRING = "Last updated: 07/24/2011";
    private static final String IMAGE_PATH_FILE = "imagelocation.qof"; //.qof - Quarantine Online File
    protected static final int IMAGE_STARTING_INDEX = 4;
    protected static final int MAX_ZOOM = 4;
    protected static final double ZOOM_COEFFICIENT = .25;

    //Global ints used for important program backends
    protected static int MAX_UNDOS = 20;
    protected static int GRID_SNAP = 10;

    //Global ints
    protected static int MAP_WIDTH = 1440;
    protected static int MAP_HEIGHT = 900;
    protected int mouseX;
    protected int mouseY;
    protected int shiftedX = 0;
    protected int shiftedY = 0;
    protected int hitboxMode = 0; //0-Frame, 1-Fill
    protected int currentZoomLevel = 1;
    protected int undoCount;

    //Global doubles
    protected double currentZoomAmount = 1;

    //Global booleans
    protected boolean gridStatus = false;
    protected boolean hitboxStatus = false;
    protected boolean hasSaved = true;
    protected boolean usingNewMap = true;
    protected boolean stickyMoving = false;
    protected boolean versionExists = true;
    protected boolean movingObject = false;
    protected boolean zoomedIn = true;
    protected boolean zoomedOut = false;
    protected boolean minimizeBrowser = true;
    protected boolean updateSQLImages = false;
    protected boolean updateImagesOnDisk;

    //Global strings 
    private static String PRIMARY_DRIVE = null;
    protected String IMAGE_PATH = null;

    //Data structures
    protected LinkedList<MapEntity> mapImageList; //for all images
    protected LinkedList<MapEntity> mapEntityList; //for other entities: score boards, spawn pts, etc
    protected LinkedList<CollideZone> mapCollideList;
    protected HashMap<String, Image> imageMap;
    protected Stack<undoNode> redoStack;
    protected LinkedList<undoNode> actions;
    protected ObjectBrowser objectBrowser;
    protected MiniMap miniMap;
    protected SQLHandler sqlH;
    protected FTPHandler ftpH;
    protected JMenuBar menuBar;
    protected File fileL;
    protected MapEntity onCursor;
    protected MapEntity lastKnownEntity;
    protected Display d;
    protected DebugMenu dM;
    protected JFrame objectBrowserFrame;

    //Menu components
    JMenu menu_file;
    JMenuItem menuItem_file_new;
    JMenuItem menuItem_file_load;
    JMenuItem menuItem_file_save;
    JMenuItem menuItem_file_saveAs;
    JMenuItem menuItem_file_exit;
    JMenu menu_edit;
    JMenuItem menuItem_edit_undo;
    JMenuItem menuItem_edit_redo;
    JMenu menu_settings;
    JMenuItem menuItem_settings_stickyMove;
    JMenuItem menuItem_settings_undoLimit;
    JMenuItem menuItem_settings_gridSnap;
    JMenu menu_tools;
    JMenu menu_tools_ftpOptions;
    JMenuItem menuItem_tools_ftpOptions_updateImages;
    JMenuItem menuItem_tools_ftpOptions_reloadConnection;
    JMenuItem menuItem_tools_ftpOptions_dropConnection;
    JMenuItem menuItem_tools_showFTPLog;
    JMenu menu_tools_SQLOptions;
    JMenuItem menuItem_tools_SQLOptions_load;
    JMenuItem menuItem_tools_SQLOptions_write;
    JMenuItem menuItem_tools_SQLOptions_update;
    JMenuItem menuItem_tools_showSQLLog;
    JMenuItem menuItem_tools_showGrid;
    JMenuItem menuItem_tools_toggleHitboxes;
    JMenuItem menuItem_tools_hitboxMode;
    JMenuItem menuItem_tools_getLastKnownObject;
    JMenuItem menuItem_tools_zoomOut;
    JMenuItem menuItem_tools_zoomIn;
    JMenuItem menuItem_tools_browser;
    JCheckBox checkBox_tools_minimizeBrowser;
    JMenu menu_create;
    JMenuItem menuItem_create_spawnPoint;
    JMenu menu_help;
    JMenuItem menuItem_help_about;

    /*
     *  First method called when the applet is loaded
     *
     *  Program logic:
     *  (If a step's number is xE, where x is the number, this is an end to the initial sequence. This means that there are no more steps to decide;
     *  the program will then load the GUI.)
     *
     *  1.  Try to find the user's current hard disk drive (ex: C:\)
     *      If:
     *          a. The drive is NOT found -> GO TO 2E
     *          b. The drive is found -> GO TO 3
     *  2E. Run the program remotely. That is, connect to the FTP server, retrieve all of the images, and put them in the Object Browser for the user to use.
     *      While running remotely, the user can still load files from their local disk, as well as save files, and upload a map to the SQL database.
     *  3.  Check to see if the IMAGE_PATH_FILE exists (ex: C:\imagelocation.qof)
     *      If:
     *          a. The file does NOT exist -> GO TO 4
     *          b. The file exists -> GO TO 5
     *  4.  Ask the user if they want to specify a path for the images to be stored, or if they want to run the program remotely
     *      If:
     *          a. They want to specify a path -> GO TO 6
     *          b. They want to run remotely -> GO TO 2
     *  5.  Try reading the path from the IMAGE_PATH_FILE (ex: C:\imagelocation.qof), to find the path where the images are stored
     *      If:
     *          a. The path exists -> GO TO 7
     *          b. The path doesn't exist (the folder was deleted) -> GO TO 6
     *  6.  Ask the user where they want to store the images (the file path), and write this location to the IMAGE_PATH_FILE
     *          GO TO 8E
     *  7.  Ask the user if they want to update their images on disk
     *      If:
     *          a. Yes -> GO TO 8E
     *          b. No -> GO TO 9E
     *  8E. Connect to the FTP server, retrieve the images, and check each one to see if it is already in the user's local directory (IMAGE_PATH).
     *      If it isn't, download it, and write it to the disk. All files that the user has, but are not on the remote end, are renamed with a ".old" extension.
     *  9E. Load the images from the user's local directory (IMAGE_PATH), and add them to the Object Browser for the user to use.
     *      The FTP server is never contacted, and the user is ready to go.
     */
    @Override
    public void init()
    {
        if(!login())
        {
            System.exit(0);
        }

        //MAP_WIDTH = Integer.parseInt(JOptionPane.showInputDialog("Set width:"));
        //MAP_HEIGHT = Integer.parseInt(JOptionPane.showInputDialog("Set height:"));

        //initialize
        undoCount = 0;
        mapImageList = new LinkedList<MapEntity>();
        mapEntityList = new LinkedList<MapEntity>();
        imageMap = new HashMap<String, Image>();
        redoStack = new Stack<undoNode>();
        actions = new LinkedList<undoNode>();
        
        //miniMap = new MiniMap(MAP_WIDTH-250, MAP_HEIGHT-250, 250, 250);
        dM = new DebugMenu();
        ftpH = new FTPHandler(this);

        //make the content display
        d = new Display(this);
        getContentPane().add(d);

        //setup menu
        setJMenuBar(createMenu());
        addListeners();

        //find disk drive and check for the image path; if no disk is found, load the images remotely
        if(Utility.FindPrimaryDrive())
        {
            PRIMARY_DRIVE = Utility.primaryDrive;
            checkForImageDirectory();
        }
        else
        {
            print("Cannot find the working disk drive. The images will be loaded remotely, rather than locally.", "Disk drive not found", JOptionPane.WARNING_MESSAGE);
            updateImagesOnDisk = false;
            loadImagesFromFTP();
        }

        //initialize specific menu items
        menuItem_tools_zoomIn.setEnabled(false);
        menuItem_edit_undo.setEnabled(false);
        menuItem_edit_redo.setEnabled(false);
        checkBox_tools_minimizeBrowser.setSelected(true);
        menuItem_settings_undoLimit.setEnabled(false);

        menuBar.validate();
    }

    //Constructs the main menu bar, adding every component to it, then returns the menu bar
    public JMenuBar createMenu()
    {
        //Not used right now
        if(!(menuBar == null))
            menuBar.removeAll();

        //Make the main menu bar
        menuBar = new JMenuBar();

        //File components
        menu_file = (JMenu)menuBar.add(new JMenu("File"));
        menuItem_file_new = menu_file.add(new JMenuItem("New"));
        menu_file.addSeparator();
        menuItem_file_load = menu_file.add(new JMenuItem("Load (From Disk)"));
        menuItem_file_save = menu_file.add(new JMenuItem("Save (To Disk)"));
        menuItem_file_saveAs = menu_file.add(new JMenuItem("Save As (To Disk)"));
        menu_file.addSeparator();
        menuItem_file_exit = menu_file.add(new JMenuItem("Exit"));

        menuItem_file_save.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, ActionEvent.CTRL_MASK));
        menuItem_file_new.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, ActionEvent.CTRL_MASK));
        menuItem_file_load.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L, ActionEvent.CTRL_MASK));
        menuItem_file_saveAs.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, ActionEvent.SHIFT_MASK));

        //Edit components
        menu_edit = (JMenu)menuBar.add(new JMenu("Edit"));
        menuItem_edit_undo = menu_edit.add(new JMenuItem("Undo"));
        menuItem_edit_redo = menu_edit.add(new JMenuItem("Redo")); 

        menuItem_edit_undo.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, ActionEvent.CTRL_MASK));
        menuItem_edit_redo.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Y, ActionEvent.CTRL_MASK));

        //Settings components
        menu_settings = (JMenu)menuBar.add(new JMenu("Settings"));
        menuItem_settings_undoLimit = menu_settings.add(new JMenuItem("Set Undo Limit"));
        menuItem_settings_stickyMove = menu_settings.add(new JMenuItem("Toggle Sticky Moving"));
        menuItem_settings_gridSnap = menu_settings.add(new JMenuItem("Change Grid Snap"));

        menuItem_settings_undoLimit.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, ActionEvent.SHIFT_MASK));
        menuItem_settings_stickyMove.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_T, ActionEvent.CTRL_MASK));
        menuItem_settings_gridSnap.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_G, ActionEvent.SHIFT_MASK));

        //Tools components
        menu_tools = (JMenu)menuBar.add(new JMenu("Tools"));
        menu_tools_ftpOptions = (JMenu)menu_tools.add(new JMenu("FTP Options"));
        menuItem_tools_ftpOptions_updateImages = menu_tools_ftpOptions.add(new JMenuItem("Update images (Works, but locks up GUI)"));
        menuItem_tools_ftpOptions_reloadConnection = menu_tools_ftpOptions.add(new JMenuItem("Reload FTP connection"));
        menuItem_tools_ftpOptions_dropConnection = menu_tools_ftpOptions.add(new JMenuItem("Drop FTP connection"));
        menuItem_tools_showFTPLog = menu_tools.add(new JMenuItem("Show FTP Log"));
        menu_tools.addSeparator();
        menu_tools_SQLOptions = (JMenu)menu_tools.add(new JMenu("SQL Options"));       
        menu_tools_SQLOptions.setEnabled(false);
        menuItem_tools_SQLOptions_load = menu_tools_SQLOptions.add(new JMenuItem("Load map from SQL database"));
        menuItem_tools_SQLOptions_write = menu_tools_SQLOptions.add(new JMenuItem("Write map to SQL database"));
        menuItem_tools_SQLOptions_update = menu_tools_SQLOptions.add(new JMenuItem("Update images (Works, but locks up GUI)"));
        menuItem_tools_showSQLLog = menu_tools.add(new JMenuItem("Show SQL Log"));
        menuItem_tools_showSQLLog.setEnabled(false);
        menu_tools.addSeparator();
        menuItem_tools_showGrid = menu_tools.add(new JMenuItem("Show Grid"));
        menuItem_tools_toggleHitboxes = menu_tools.add(new JMenuItem("Toggle Hitboxes"));
        menuItem_tools_hitboxMode = menu_tools.add(new JMenuItem("Change Hitbox Mode"));
        menuItem_tools_getLastKnownObject = menu_tools.add(new JMenuItem("Get Last Known Object"));
        menu_tools.addSeparator();
        menuItem_tools_zoomOut = menu_tools.add(new JMenuItem("Zoom Out"));
        menuItem_tools_zoomIn = menu_tools.add(new JMenuItem("Zoom In"));
        menu_tools.addSeparator();
        menuItem_tools_browser = menu_tools.add(new JMenuItem("Object Browser"));
        checkBox_tools_minimizeBrowser = new JCheckBox("Minimize browser when object is selected");
        menu_tools.add(checkBox_tools_minimizeBrowser);

        menuItem_tools_showGrid.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, ActionEvent.CTRL_MASK));
        menuItem_tools_toggleHitboxes.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_H, ActionEvent.CTRL_MASK));
        menuItem_tools_hitboxMode.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_H, ActionEvent.SHIFT_MASK));
        menuItem_tools_getLastKnownObject.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_G, ActionEvent.CTRL_MASK));
        menuItem_tools_zoomOut.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, ActionEvent.CTRL_MASK));
        menuItem_tools_zoomIn.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W, ActionEvent.CTRL_MASK));
        menuItem_tools_browser.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F, ActionEvent.CTRL_MASK)); 
        
        //Create components
        menu_create = (JMenu)menuBar.add(new JMenu("Create"));
        menuItem_create_spawnPoint = menu_create.add(new JMenuItem("Spawn Point"));
        
        //Help components
        menu_help = (JMenu)menuBar.add(new JMenu("Help"));
        menuItem_help_about = menu_help.add(new JMenuItem("About"));
        
        return menuBar;
    }

    //Adds an actionlistener to each menu component
    public void addListeners()
    {
        //File
        menuItem_file_new.addActionListener(this);
        menuItem_file_load.addActionListener(this);
        menuItem_file_save.addActionListener(this);
        menuItem_file_saveAs.addActionListener(this);
        menuItem_file_exit.addActionListener(this);

        //Edit
        menuItem_edit_undo.addActionListener(this);
        menuItem_edit_redo.addActionListener(this);
        
        //Settings
        menuItem_settings_undoLimit.addActionListener(this);
        menuItem_settings_stickyMove.addActionListener(this);
        menuItem_settings_gridSnap.addActionListener(this);

        //Tools
        menuItem_tools_ftpOptions_updateImages.addActionListener(this);
        menuItem_tools_ftpOptions_reloadConnection.addActionListener(this);
        menuItem_tools_ftpOptions_dropConnection.addActionListener(this);
        menuItem_tools_showFTPLog.addActionListener(this);
        menuItem_tools_SQLOptions_load.addActionListener(this);
        menuItem_tools_SQLOptions_write.addActionListener(this);
        menuItem_tools_SQLOptions_update.addActionListener(this);
        menuItem_tools_showSQLLog.addActionListener(this);
        menuItem_tools_showGrid.addActionListener(this);
        menuItem_tools_toggleHitboxes.addActionListener(this);
        menuItem_tools_hitboxMode.addActionListener(this);
        menuItem_tools_getLastKnownObject.addActionListener(this);
        menuItem_tools_zoomOut.addActionListener(this);
        menuItem_tools_zoomIn.addActionListener(this);
        menuItem_tools_browser.addActionListener(this);
        checkBox_tools_minimizeBrowser.addActionListener(this);
        
        //Create
        menuItem_create_spawnPoint.addActionListener(this);

        //Help
        menuItem_help_about.addActionListener(this);
    }

    //Called when any of the menu objects are clicked on
    public void actionPerformed(ActionEvent e)
    {
        String event = e.getActionCommand();
        Object source = e.getSource();

        if(source.equals(menuItem_file_new))
        {
            newMap(true);
        }
        else if(source.equals(menuItem_file_load))
        {
            loadMap();
        }
        else if(source.equals(menuItem_file_save))
        {
            saveMap();
        }
        else if(source.equals(menuItem_file_saveAs))
        {
            saveAsMap();
        }
        else if(source.equals(menuItem_file_exit))
        {
            System.exit(0);
        }
        else if(source.equals(menuItem_edit_undo))
        {
            undo();
        }
        else if(source.equals(menuItem_edit_redo))
        {
            redo();
        }
        else if(source.equals(menuItem_settings_undoLimit))
        {
            setUndoLimit();
        }
        else if(source.equals(menuItem_settings_stickyMove))
        {
            if(stickyMoving)
            {
                stickyMoving = !stickyMoving;
                JOptionPane.showMessageDialog(null, "Sticky moving is now turned OFF. Objects will no longer create a copy on the mouse after moving them.");
            }
            else
            {
                stickyMoving = !stickyMoving;
                JOptionPane.showMessageDialog(null, "Sticky moving is now turned ON. Objects that are moved will now create a copy on the mouse.");
            }
        }
        else if(source.equals(menuItem_settings_gridSnap))
        {
            setGridSnap();
        }
        else if(source.equals(menuItem_tools_ftpOptions_updateImages))
        {
            updateImagesOnDisk = true;
            loadImagesFromFTP();
        }
        else if(source.equals(menuItem_tools_ftpOptions_reloadConnection))
        {
            reloadConnection();
        }
        else if(source.equals(menuItem_tools_ftpOptions_dropConnection))
        {
            if(ftpH == null || !ftpH.isConnected())
            {
                JOptionPane.showMessageDialog(null, "There is currently no connection to " + HOST_NAME + ".");
                return;
            }
            if(dropConnection())
                JOptionPane.showMessageDialog(null, "FTP connection was dropped from " + HOST_NAME + ".");
        }
        else if(source.equals(menuItem_tools_showFTPLog))
        {
            if(FTPHandler.getFTPLogger() == null)
                print("FTP connection was never made.");
            else
                FTPHandler.getFTPLogger().showLog();
        }
        else if(source.equals(menuItem_tools_SQLOptions_load))
        {
            loadMapFromSQL();
        }
        else if(source.equals(menuItem_tools_SQLOptions_write))
        {
            writeMapToSQL();
        }
        else if(source.equals(menuItem_tools_SQLOptions_update))
        {
            updateSQLImages();
        }
        else if(source.equals(menuItem_tools_showSQLLog))
        {
            if(FTPHandler.getSQLLogger() == null)
                print("SQL connection was never made.");
            else
                FTPHandler.getSQLLogger().showLog();
        }
        else if(source.equals(menuItem_tools_showGrid))
        {
            gridStatus = !gridStatus;
        }
        else if(source.equals(menuItem_tools_toggleHitboxes))
        {
            hitboxStatus = !hitboxStatus;
        }
        else if(source.equals(menuItem_tools_hitboxMode))
        {
            if(hitboxMode == 0)
                hitboxMode = 1;
            else
                hitboxMode = 0;
        }
        else if(source.equals(menuItem_tools_getLastKnownObject))
        {
            if(movingObject)
            {
                print("Can't do this action while moving an object.");
                return;
            }

            if(lastKnownEntity == null)
                print("No objects have been selected this session.");
            else
                onCursor = lastKnownEntity;
        }
        else if(source.equals(menuItem_tools_zoomOut))
        {
            menuItem_tools_zoomIn.setEnabled(true);
            zoomedIn = false;

            currentZoomAmount -= ZOOM_COEFFICIENT;
            currentZoomLevel++;

            if(currentZoomLevel == MAX_ZOOM)
            {
                zoomedOut = true;           
                menuItem_tools_zoomOut.setEnabled(false);
            }

            menuBar.validate();
        }
        else if(source.equals(menuItem_tools_zoomIn))
        {
            menuItem_tools_zoomOut.setEnabled(true);
            zoomedOut = false;

            currentZoomAmount += ZOOM_COEFFICIENT;
            currentZoomLevel--;
            
            if(currentZoomLevel == 1)
            {
                zoomedIn = true;
                menuItem_tools_zoomIn.setEnabled(false); 
            }

            menuBar.validate();  
        }
        else if(source.equals(menuItem_tools_browser))
        {
            openBrowser();
        }
        else if(source.equals(checkBox_tools_minimizeBrowser))
        {
            if(checkBox_tools_minimizeBrowser.isSelected())
                minimizeBrowser = true;
            else
                minimizeBrowser = false;
        }
        else if(source.equals(menuItem_create_spawnPoint))
        {
            createSpawnPoint();
        }
        else if(source.equals(menuItem_help_about))
        {
            print(VERSION_STRING + "\n" + LAST_UPDATED_STRING + "\n\nDeveloped by Brian Thomas", "About Quarantine Editor", JOptionPane.INFORMATION_MESSAGE);
        }
        else
        {
            print("This has not been implemented yet.");
        }
        repaint();
    }

    //Called at the start of the program for a user to login in with a username and password
    public boolean login()
    {
        return true;
    }

    /*  
     *  Called if a drive is found in init(), or whenever the image directory needs to be looked for
     *
     *  Tries to find the image directory on the user's computer by searching for the IMAGE_PATH_FILE on the primary drive (ex: C:\imagelocation.qof)
     *  If IMAGE_PATH_FILE doesn't exist, a method is called to prompt the user to enter the IMAGE_PATH and it will save that to the IMAGE_PATH_FILE
     *  If IMAGE_PATH_FILE does exist, the method reads from it to get IMAGE_PATH, and then loads the images
     */
    public void checkForImageDirectory()
    {
        String outputFileName = "";

        try
        {
            //Double-checks to make sure IMAGE_PATH_FILE exists and PRIMARY_DRIVE isn't null, so bad things don't happen to C:\!
            if(IMAGE_PATH_FILE != null && !IMAGE_PATH_FILE.equals("") && PRIMARY_DRIVE != null)
                outputFileName = PRIMARY_DRIVE + IMAGE_PATH_FILE;
            else
            {
                throw new Exception("Error when checking for path: Image path file is non-existent.");
            }

            //Creates a new file at outputFileName location (ex: C:\imagelocation.qof)
            File outputFile = new File(outputFileName);

            /* 
             *  If outputFile (ex: C:\imagelocation.qof) doesn't exist, call assignImageDirectory(), which will prompt the user to choose a path to download the 
             *  images to, and create the outputFile at that location.
             *  If outputFile does exist, read from it to get the IMAGE_PATH
             */
            if(!outputFile.exists())
            {
                assignImageDirectory();
            }
            else
            {
                //Reads the image path from the file
                BufferedReader in = new BufferedReader(new FileReader(outputFile));
                IMAGE_PATH = in.readLine();
                in.close();

                //If the image path (that was read from the outputFile) doesn't actually exist on the user's computer, tell the user to choose a new path.
                if(!new File(IMAGE_PATH).exists())
                {
                    print("The path:\n\n\"" + IMAGE_PATH + "\"\n\ndoes not exist! The program will now let you reassign the path. \n(If the folder was moved, " +
                            "just select the new location, and the folder will update correctly)");

                    //Sets the IMAGE_PATH back to null, so there is no conflict
                    IMAGE_PATH = null;

                    //Call assignImageDirectory() to let the user choose a new image directory to save images to
                    assignImageDirectory();
                    return;
                }
                
                /*
                 *  Asks the user if they would like to update their local images
                 *  If yes, calls loadImagesFromFTP() to update the user's images
                 *  If no, the images will be loaded locally from IMAGE_PATH
                 */
                String[] options = {"Yes", "No"};
                int choice = JOptionPane.showOptionDialog(null, "Your current image path is: " + "\n" + IMAGE_PATH + "\n\nWould like to update your local images?",
                        "Update Image Notification", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options , options[0]);
                if(choice == 0)
                {
                    updateImagesOnDisk = true;
                    loadImagesFromFTP();
                }
                else
                {
                    loadImagesLocally();
                }
            }
        }
        catch(Exception e)
        {
            print("Error when checking for directory. Error: " + e.getMessage());
        }
    }

    /*
     *  Called if IMAGE_PATH has not yet been assigned
     *
     *  Prompts the user to choose an option:
     *  1. Specify a path for the images to be saved to: In this case, the user will use a FileChooser to find a directory, and choose an already created folder.
     *  writeImagePathToFile() will then be called to write this path to the IMAGE_PATH_FILE location (ex: C:\imagelocation.qof).
     *  2. Run the program remotely: In this case, the IMAGE_PATH is ignored, and nothing is written to the IMAGE_PATH_FILE. The editor is then run remotely,
     *  which means that all of the images are downloaded from the FTP, and will cease to exist after the program is terminated.
     */
    public void assignImageDirectory()
    {
        //Asks the user if they want to create a path for the images to be stored at, or if they want to run the program remotely
        String[] options = {"Specify a path", "Run remotely"};
        int choice = JOptionPane.showOptionDialog(null, "No image path was found on this hard drive.\n\nSpecify a path now, or run remotely?", "No Path Found",
                JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE, null, options , options[0]);
        
        //If the user decides to specify a path
        if(choice == 0)
        {
            JFileChooser chooser;
            try
            {
                chooser = new JFileChooser();
            }
            catch(Exception e)
            {
                JOptionPane.showMessageDialog(null, "Error loading file prompt.");
                return;
            }
            //Set the prefences for the FileChooser. It is an "Open" dialog, showing directories only.
            chooser.setDialogType(JFileChooser.OPEN_DIALOG);
            chooser.setDialogTitle("Choose directory to save images in:");
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            int option = chooser.showOpenDialog(this);

            //If the user has chosen a path successfully
            if(option == JFileChooser.APPROVE_OPTION)
            {
                //If the user selected a valid directory
                if(chooser.getSelectedFile().isDirectory())
                {
                    //Assigns IMAGE_PATH to the path the user selected, which will get written to the IMAGE_PATH_FILE
                    IMAGE_PATH = chooser.getSelectedFile().toString();
                    int choice2 = JOptionPane.showConfirmDialog(null, "You have selected the following directory:\n" + IMAGE_PATH + "\n\nTo be safe, please choose " +
                        "an empty directory.\n(Unless this directory already contains images that were previously downloaded)" + "\n\nContinue with this directory?",
                            "Are you sure?", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);

                    //If the user changes their mind about the chosen directory, call assignImageDirectory() again, and return the current one to avoid recursion.
                    if(choice2 == JOptionPane.NO_OPTION)
                    {
                        assignImageDirectory();
                        return;
                    }
                    //Call writeImagePathToFile() to write the newly assigned IMAGE_PATH to the IMAGE_PATH_FILE and set updateImagesOnDisk to true,
                    //so when loadImagesFromFTP() is called, it will update the IMAGE_PATH (effectively downloading all the images to the path)
                    writeImagePathToFile();
                    updateImagesOnDisk = true;
                }
                //If the user selected a nonexisting directory (or a file, which FileChooser should disallow), call assignImageDirectory() again
                else
                {
                    print("An existing directory is required.", "Invalid directory", JOptionPane.ERROR_MESSAGE);
                    assignImageDirectory();
                    return;
                }
            }
            //If the user closed out of the FileChooser, call assignImageDirectory() again
            else
            {
                assignImageDirectory();
                return;
            }
        }
        //If the user decides to run remotely, set updateImagesOnDisk to false, so loadImagesFromFTP() will know not to check for a directory to update
        else if(choice == 1)
            updateImagesOnDisk = false;
        //If the user closes the dialog without making a choice
        else
            System.exit(0);

        loadImagesFromFTP();
    }

    /*
     * Called
     */
    public void writeImagePathToFile()
    {
        try
        {
            FileWriter writer = new FileWriter(new File(PRIMARY_DRIVE + "\\" + IMAGE_PATH_FILE));
            PrintWriter out = new PrintWriter(writer);
            out.println(IMAGE_PATH);
            out.close();
            writer.close();
        }
        catch(Exception e)
        {
            print("Error when writing image path to file. Error: " + e.getMessage());
        }
    }

    //Minimizes the Object Browser
    public void minimizeBrowser()
    {
        if(minimizeBrowser)
            objectBrowserFrame.setState(JFrame.ICONIFIED);
    }

    //Called when menu button is clicked
    public void openBrowser() 
    {
        if(objectBrowserFrame.getState() == JFrame.ICONIFIED)
            objectBrowserFrame.setState(JFrame.NORMAL);
        objectBrowserFrame.setVisible(true);
    }

    //Closes the Object Browser
    public void closeBrowser()
    {
        objectBrowserFrame.setVisible(false);
    }

    //Constructs objectBrowser in a frame
    public void setupBrowser()
    {
        //Create frame for browser
        objectBrowserFrame = new JFrame("Object Browser");

        //Add content to the windows
        //frame.add(objectBrowser.objectBrowser, BorderLayout.CENTER);
        JComponent objectBrowserPane = objectBrowser.getObjectBrowser();
        objectBrowserPane.setOpaque(true); //content panes must be opaque
        objectBrowserFrame.setContentPane(objectBrowserPane);

        //Set up display
        objectBrowserFrame.setPreferredSize(new Dimension(809, 800));
        objectBrowserFrame.pack();
        objectBrowserFrame.setResizable(false);
        objectBrowserFrame.getContentPane().setBackground(Color.WHITE);
    }
    
    //Connects to the FTP server and fetches all of the map images
    //Returns true if connected; false if an error occurs
    public boolean loadImagesFromFTP()
    {
        if(objectBrowser != null)
            closeBrowser();

        //If user wants to update and there are images drawn on the screen, save and clear all images, in case certain images in the repository
        //get deleted with the update
        if(mapImageList.size() != 0 )
        {
            int choice = JOptionPane.showConfirmDialog(null, "Before updating, you must clear the map. If you do not save, all of your current data will be lost. " +
                    "\n\nSave now?", "Save before updating?", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
            if(choice == JOptionPane.YES_OPTION)
                saveMap();
            else if(choice == JOptionPane.CANCEL_OPTION)
                return true;

            newMap(false);
        }
        
        //Reset all data before calling loadImages
        imageMap.clear();
        objectBrowser = new ObjectBrowser(this);
        setupBrowser();
        
        //Called if the update boolean is set to true by a method, but the image_path was never found (there is no file)
        if(updateImagesOnDisk && IMAGE_PATH == null)
        {
            print("Cannot find the image directory. The images will be loaded remotely, rather than locally.", "Image directory not found", JOptionPane.WARNING_MESSAGE);
            updateImagesOnDisk = false;
        }

        //Call loadImages with 2 booleans. If updateSQLImages is true, it will update them.
        //If updateImagesOnDisk is true, the images on the user's disk will be updated. If false, the program will load the images remotely
        return ftpH.loadImages(updateSQLImages, updateImagesOnDisk);
    }

    public boolean loadImagesLocally()
    {
        objectBrowser = new ObjectBrowser(this);
        setupBrowser();
        return true;
    }

    //Simply reconnects to the FTP server. Doesn't retrieve messages or images
    public boolean reloadConnection()
    {
        QuickFrame frame = new QuickFrame("Reconnecting", "Reconnecting to server...");
        try
        {
            ftpH.reloadConnection();
        }
        catch(Exception e)
        {
            print("Error reconnecting to " + HOST_NAME + ".");
            frame.dispose();
            return false;
        }
        frame.dispose();
        return true;
    }

    //Drops current FTP connection
    public boolean dropConnection()
    {
        try
        {
            ftpH.disconnect();
        }
        catch(Exception e)
        {
            JOptionPane.showMessageDialog(null, "Error disconnecting from FTP server. Error: " + e);
            return false;
        }

        return true;
    }

    //Attempts to get the current map from the SQL database and load it into the Editor
    public void loadMapFromSQL()
    {
        QuickFrame frame = new QuickFrame("Downloading", "Downloading from SQL Database...");

        //Reset everything
        mapImageList = new LinkedList<MapEntity>();
        repaint();
        redoStack = new Stack<undoNode>();
        actions = new LinkedList<undoNode>();
        undoCount = 0;
        hasSaved = true;
        shiftedX = 0;
        shiftedY = 0;
        fileL = null;

        try
        {
            //Connect to the DB and get the list of images and the corresponding ID map with names
            sqlH = new SQLHandler();
            LinkedList<SQLMapEntity> objectList = sqlH.getStaticObjects();

            if(objectList.size() == 0)
                throw new Exception("There are no objects in the map table.");

            Map<Integer, String> objectMap = sqlH.getObjectMap();

            //For each object in the map, get the name by accessing the objectMap with the object's ID
            //Then, get the image from Editor's imageMap, and with that width/height, construct a mapEntity to put in mapImageList
            for(SQLMapEntity sme : objectList)
            {
                String objectName = objectMap.get(sme.getImageId());
                BufferedImage image = (BufferedImage)imageMap.get(objectName);
                mapImageList.add(new BackgroundEntity(objectName, sme.getX(), sme.getY(), image.getWidth(), image.getHeight()));
            }

            sqlH.close();
        }
        catch(Exception e)
        {
            print("Error retrieving information from the SQL Database. Exception: " + e.getMessage());
            //e.printStackTrace();
        }
        
        frame.dispose();
        usingNewMap = true;
    }

    //Attemps to connect to the SQL DB and write the current map's contents to the DB
    public void writeMapToSQL()
    {
        saveMap();
        
        if(fileL == null)
            return;

        QuickFrame frame = new QuickFrame("Writing", "Writing to SQL Database");

        try
        {
            //Connect to the DB and get the list of images and the corresponding ID map with names
            sqlH = new SQLHandler();
            sqlH.clearCurrentMap();
            Map<String, Integer> objectMap = sqlH.getReverseObjectMap();

            for(MapEntity mo : mapImageList)
            {
                sqlH.insertIntoMap(objectMap.get(mo.getImageName()), mo.getX(), mo.getY());
            }
        }
        catch(Exception e)
        {
            //print("Error loading SQLHandler. Exception: " + e.getMessage());
            e.printStackTrace();
        }

        sqlH.close();
        frame.dispose();

        usingNewMap = false;
    }

    //Attempts to connect to the SQL DB, and update the images stored there with the most current images from ***REMOVED***
    private void updateSQLImages()
    {
        boolean update = true;

        //Ask user if they want to update their images on disk
        String[] options = {"Yes", "No"};
        int choice = JOptionPane.showOptionDialog(null, "Would you like to update your local images too?", "Image Update Notification",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options , options[0]);
        if(choice == 0)
            updateImagesOnDisk = true;
        else
        {
            updateImagesOnDisk = false;
            update = false;
        }
            
        //If the user doesn't want to update the local images
        if(!update)
        {
            //Inform user that it will refresh object browser and current images
            String[] options2 = {"Ok", "Cancel"};
            int choice2 = JOptionPane.showOptionDialog(null, "Updating the SQL images will refresh your Object Browser with the most current images.", "Image Refresh",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.INFORMATION_MESSAGE, null, options2 , options2[0]);
            if(choice2 != 0)
                return;
        }
 
        updateSQLImages = true;
        if(!loadImagesFromFTP())
        {
            print("Error globally connecting to the FTP server.");
        }
        updateSQLImages = false;
    }

    //Used for testing
    public void getSomeImages()
    {
        try
        {
            Logger imageLogger = new Logger("Fetching", 24, 30);
            imageLogger.showLog();

            long time1 = System.nanoTime();
            BufferedImage currentImage = ImageIO.read(new URL("http://***REMOVED***/edit/objects/Blood/blood1.png"));
            imageLogger.addLine("Loaded " + "blood1.png" + " (" +
                    Utility.RoundToDecimalPlace(((System.nanoTime() - time1)/1000000000.0), 3) + " seconds)");

            time1 = System.nanoTime();
            BufferedImage currentImage2 = ImageIO.read(new URL("http://***REMOVED***/edit/objects/Blood/blood2.png"));
            imageLogger.addLine("Loaded " + "blood2.png" + " (" +
                    Utility.RoundToDecimalPlace(((System.nanoTime() - time1)/1000000000.0), 3) + " seconds)");

            time1 = System.nanoTime();
            BufferedImage currentImage3 = ImageIO.read(new URL("http://***REMOVED***/edit/objects/Blood/blood2.png"));
            imageLogger.addLine("Loaded " + "blood3.png" + " (" +
                    Utility.RoundToDecimalPlace(((System.nanoTime() - time1)/1000000000.0), 3) + " seconds)");

            time1 = System.nanoTime();
            BufferedImage currentImage4 = ImageIO.read(new URL("http://***REMOVED***/edit/objects/Blood/blood2.png"));
            imageLogger.addLine("Loaded " + "blood4.png" + " (" +
                    Utility.RoundToDecimalPlace(((System.nanoTime() - time1)/1000000000.0), 3) + " seconds)");

            time1 = System.nanoTime();
            BufferedImage currentImage5 = ImageIO.read(new URL("http://***REMOVED***/edit/objects/Blood/blood2.png"));
            imageLogger.addLine("Loaded " + "blood5.png" + " (" +
                    Utility.RoundToDecimalPlace(((System.nanoTime() - time1)/1000000000.0), 3) + " seconds)");
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }

    //Creates a new map, resetting all values and clearing the canvas
    public void newMap(boolean check)
    {
        //Call saveCheck; if they want to save, call save().
        if(check)
            if(saveCheck())
                saveMap();

        undoCount = 0;
        mapImageList = new LinkedList<MapEntity>();
        redoStack = new Stack<undoNode>();
        actions = new LinkedList<undoNode>();
        fileL = null;
        hasSaved = true;
        usingNewMap = true;

        menuItem_edit_undo.setEnabled(false);
        menuItem_edit_redo.setEnabled(false);
        menuBar.validate();
        
        repaint();
    }
    
    //Reads from a map that the user chooses and adds each object into the mapImageList
    public void loadMap()
    {
        boolean outdated = false;
        String oldObjects = "";

        //If the user hasn't saved, call save().
        if(saveCheck())
            saveMap();

        JFileChooser chooser;
        try
        {
            chooser = new JFileChooser();
        }
        catch(Exception e)
        {
            print("An error occurred while creating a File Chooser in loadMap. Error: " + e.getMessage());
            return;
        }
        chooser.setDialogType(JFileChooser.OPEN_DIALOG);
        chooser.setDialogTitle("Choose map to load:");
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Quarantine Online Maps", "qom");
        chooser.setFileFilter(filter);
        chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        int choice = chooser.showOpenDialog(this);

        //If the user selects something, rather than canceling
        if (choice == JFileChooser.APPROVE_OPTION)
        {
            fileL = (File)chooser.getSelectedFile();
            //If the file doesn't have an extension
            if(fileL.toString().lastIndexOf(".") == -1) 
            {
                JOptionPane.showMessageDialog(null, "That is not the proper map format.");
                loadMap();
                return;
            }
            //If the file's extension is not equal to map_file_extension
            if(!fileL.toString().substring((fileL.toString()).lastIndexOf(".")).equals(MAP_FILE_EXTENSION))
            {
                JOptionPane.showMessageDialog(null, "That is not the proper map format.");
                loadMap();
                return;
            }
            try
            {
                mapImageList = new LinkedList<MapEntity>();
                repaint();
                redoStack = new Stack<undoNode>();
                actions = new LinkedList<undoNode>();
                BufferedReader in = new BufferedReader(new FileReader(fileL));
                String input = in.readLine();
                while (input != null)
                {
                    StringTokenizer tz = new StringTokenizer(input);
                    int entityType = Integer.parseInt(tz.nextToken());

                    String imageName = tz.nextToken();

                    //If the method made it this far, the file is a .qom file
                    //However, if the image name on the current line isn't in the imageMap, it doesn't exist in the user's image database, so
                    //a string is then created to document all of the cases of old objects
                    if(imageMap.get(imageName) == null)
                    {
                        if(!oldObjects.contains(imageName))
                            oldObjects += ("\n" + imageName);
                        outdated = true;
                    }
                    else
                    {
                        //Get the info about the MapEntity, and add it to the mapImageList
                        int x = ((Integer)Integer.parseInt(tz.nextToken())).intValue();
                        int y = ((Integer)Integer.parseInt(tz.nextToken())).intValue();
                        int width = -1;
                        int height = -1;
                        while ( width == -1 || height == -1)
                        {
                            width = imageMap.get(imageName).getWidth(this);
                            height = imageMap.get(imageName).getHeight(this);
                        }

                        //If the line begins with BackgroundEntity
                        if(entityType == MapEntity.EntityType.BACKGROUND.ordinal())
                        {
                            BackgroundEntity temp = new BackgroundEntity(imageName, x, y, width, height);
                            temp.setHitBox();
                            mapImageList.add(temp);
                        }
                        else if(entityType == MapEntity.EntityType.COLLIDABLE.ordinal())
                        {
                            CollidableEntity temp = new CollidableEntity(imageName, x, y, width, height);
                            temp.setHitBox();
                            mapImageList.add(temp);
                        }
                    }
                    input = in.readLine();
                }
                if(!outdated)
                {
                    undoCount = 0;
                    usingNewMap = false;
                    hasSaved = true;
                    shiftedX = 0;
                    shiftedY = 0;
                }
                else
                    throw new Exception("Outdated");
               
            }
            catch (Exception e)
            {
                if(e.getMessage().equals("Outdated"))
                {
                    int option = JOptionPane.showConfirmDialog(null, "There are one or more images missing.\nEither this map file is outdated, or your local " +
                        "image repository is out of date. The following objects do not exist in your repository:" + oldObjects + "\n\nWould you like to " +
                            "update your image repository?\n(If this message appears after an update, then the map file is out of date.)", "Could not load map: " +
                                "Images missing", JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE);
                    if(option == JOptionPane.YES_OPTION)
                    {
                        updateImagesOnDisk = true;
                        loadImagesFromFTP();
                    }
                    newMap(true);
                }
                else
                    print("Corrupted map file.");
                
                mapImageList.clear();
                undoCount = 0;
                hasSaved = true;
                shiftedX = 0;
                shiftedY = 0;
            }
        }        
    }

    //If the user is using a map that was loaded or they already saved, it automatically saves as that file name; else, it calls the saveAs
    public void saveMap()
    {
        try
        {
            if (usingNewMap == false) 
            {
                PrintWriter out = new PrintWriter(new FileWriter(fileL));

                Iterator iter = mapImageList.iterator();
                while ( iter.hasNext())
                {
                    out.println(iter.next());
                }
                print("Saved as " + fileL.getName());
                out.close();
                hasSaved = true;
            }
            else
                saveAsMap();
            
        }
        catch (Exception e)
        {
            JOptionPane.showMessageDialog(null, "There was an error while saving the file. Using save().");
            hasSaved = true;
            newMap(true);
        }
    }

    //Prompts the user to choose a location to save the map to
    public void saveAsMap()
    {
        JFileChooser chooser;
        try
        {
            chooser = new JFileChooser();
        }
        catch(Exception e)
        {
            JOptionPane.showMessageDialog(null, "Error loading File Chooser in saveAsMap. Error: " + e.getMessage());
            return;
        }
        chooser.setDialogType(JFileChooser.SAVE_DIALOG);
        chooser.setDialogTitle("Enter name to save as:");
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Quarantine Online Maps", "qom");
        chooser.setFileFilter(filter);
        chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        int choice = chooser.showOpenDialog(this);
        
        if (choice == JFileChooser.APPROVE_OPTION)
        {
            File tempFile = (File)chooser.getSelectedFile();
            
            //If the file's extension is not equal to map_file_extension or if the file doesn't have an extension
            if(tempFile.toString().lastIndexOf(".") == -1 || !tempFile.toString().substring((tempFile.toString()).lastIndexOf(".")).equals(MAP_FILE_EXTENSION))
            {
                JOptionPane.showMessageDialog(null, "That is not the proper map format.");
                saveAsMap();
                return;
            }
            String caughtFileName = "";
            try
            {
                caughtFileName = tempFile.getName();
                //If the file doesn't exist, create it and write the map's contents to it
                if(tempFile.createNewFile())
                {
                    PrintWriter out = new PrintWriter(new FileWriter(tempFile));
                    Iterator<MapEntity> iter = mapImageList.iterator();
                    while(iter.hasNext())
                    {
                        out.println(iter.next());
                    }
                    out.close();
                    hasSaved = true;
                    usingNewMap = false;
                    fileL = tempFile;
                }
                //If the file already exists, throw an exception
                else
                    throw new Exception("File Exists");
            }
            catch(Exception ex)
            {
                //If the file already exists, tell the user, and give them an option to overwrite it
                if(ex.getMessage().equals("File Exists"))
                {
                    int n = JOptionPane.showConfirmDialog(null, (caughtFileName) + " already exists. Overwrite?", "File Exists", JOptionPane.YES_NO_OPTION);
                    if(n == JOptionPane.YES_OPTION)
                    {
                        if(usingNewMap && mapImageList.size() == 0)
                        {
                            print("Cannot save over a file while using a blank map.");
                            saveAsMap();
                        }
                        else
                        {
                            fileL = tempFile;
                            usingNewMap = false;
                            saveMap();
                        }
                    }
                    else
                        saveAsMap();
                }
                else
                {
                    print("There was an error while saving the file. Error: " + ex);
                    hasSaved = true;
                    newMap(true);
                }
            }
        }
    }

    //Checks to see if the user already saved.
    //If they have saved or if they don't want to save, return false. If they haven't saved, but they want to save, return true.
    public boolean saveCheck()
    {
        if(hasSaved)
            return false;
        int n = JOptionPane.showConfirmDialog(null, "Would you like to save your work first?", "Save?", JOptionPane.YES_NO_OPTION);
        if(n == JOptionPane.YES_OPTION)
            return true;
        return false;
    }

    //Sets the MapEntity on the cursor
    public void setOnCursor(MapEntity mo)
    {
        onCursor = mo;
    }

    //Creates a new entity and assigns it to lastKnownEntity
    public void setLastKnownEntity(MapEntity ent)
    {
        if(ent instanceof BackgroundEntity)
            lastKnownEntity = new BackgroundEntity((BackgroundEntity)ent);
        else if(ent instanceof CollidableEntity)
            lastKnownEntity = new CollidableEntity((CollidableEntity)ent);
    }

    //Adds an action, as an undoNode, to the actions list
    private void addToActions(undoNode node)
    {
        if(actions.size() == MAX_UNDOS)
        {
            actions.removeFirst();
            actions.add(node);
        }
        else
            actions.add(node);
    }

    //Called when the menu button is pressed
    //Attemps to undo the last action the user performed
    public void undo()
    {
        if(!actions.isEmpty())
        {
            if(undoCount != MAX_UNDOS)
            {
                undoNode lastAction = actions.removeLast();
                redoStack.push(lastAction);

                //If the type was add
                if(lastAction.getType() == 0)
                {
                    mapImageList.remove(lastAction.getObject());
                }
                //If the type was delete
                else if(lastAction.getType() == 1)
                {
                    mapImageList.add(lastAction.getIndex(), lastAction.getObject());
                }
                //If the type was move
                else if(lastAction.getType() == 2)
                {
                    mapImageList.remove(lastAction.getNewObject());
                    mapImageList.add(lastAction.getIndex(), lastAction.getObject());
                }

                undoCount++;
                hasSaved = false;
                menuItem_edit_redo.setEnabled(true);

                //If the maximum of undos has now been met
                if(undoCount == MAX_UNDOS || mapImageList.size() == 0 || actions.size() == 0)
                    menuItem_edit_undo.setEnabled(false);
            }
            else
            {
                JOptionPane.showMessageDialog(null, "Undo limit reached: " + undoCount + " undos", "Undo limit reached", JOptionPane.ERROR_MESSAGE);
            }
            repaint();
        }     
    }

    //Attempts to redo the last action the user 'undid'
    private void redo()
    {
        if(undoCount > 0)
        {
            undoNode lastAction = redoStack.pop();
            actions.add(lastAction);

            //If the type was add
            if(lastAction.getType() == 0)
            {
                mapImageList.add(lastAction.getObject());
            }
            //If the type was delete
            else if(lastAction.getType() == 1)
            {
                mapImageList.remove(lastAction.getObject());
            }
            //If the type was move
            else if(lastAction.getType() == 2)
            {
                MapEntity temp = lastAction.getNewObject();
                mapImageList.add(temp);
                mapImageList.remove(lastAction.getObject());
            }
            undoCount--;
            menuItem_edit_undo.setEnabled(true);

            if(undoCount == 0)
                menuItem_edit_redo.setEnabled(false);
            
            repaint();
        }
        else
        {
            JOptionPane.showMessageDialog(null, "Nothing to redo.");
        }
    }

    //Removes all objects from the redoStack
    public void emptyRedoStack()
    {
        redoStack.removeAllElements();
    }

    //Called when a new map is created, or something similar
    public void resetUndos()
    {
        menuItem_edit_undo.setEnabled(true);
        menuItem_edit_redo.setEnabled(false);
        redoStack.removeAllElements();
        undoCount = 0;
    }

    //Called when the applet is given focus
    @Override
    public void start()
    {
        
    }
    
    //Called when an applet is closed
    @Override
    public void destroy()
    {
        if(saveCheck())
            saveMap();
    }

    //Called when the user no longer gives this applet focus
    @Override
    public void stop()
    {

    }

    //Sets the grid snap to what the user specifies
    private void setGridSnap()
    {
        String in = "";

        in = JOptionPane.showInputDialog("Current grid snap: " + GRID_SNAP + "\n\nSet a new grid snap (1 is no snap):");

        if(in == null)
            return;

        try
        {
            int newSnap = Integer.parseInt(in);
            if(newSnap < 1)
            {
                JOptionPane.showMessageDialog(null, "You cannot set grid snap to a number less than 1.");
                setGridSnap();
            }
            else
                GRID_SNAP = newSnap;
        }
        catch(NumberFormatException e)
        {
            JOptionPane.showMessageDialog(null, "A number (integer) is required.");
            setGridSnap();
        }
    }

    //Sets the undo limit to what the user specifies
    private void setUndoLimit()
    {
        String in = "";
        if(MAX_UNDOS == -1)
        {
            in = JOptionPane.showInputDialog("Current undo limit: Infinite\n\nSet a new undo limit (-1 to set to unlimited):");
        }
        else
            in = JOptionPane.showInputDialog("Current undo limit: " + MAX_UNDOS + "\n\nSet a new undo limit (-1 to set to unlimited):");

        if(in == null)
            return;
        try
        { 
            int undos = Integer.parseInt(in);
            if(undos < -1)
            {
                JOptionPane.showMessageDialog(null, "Please enter a positive number (or -1 for infinite undos)");
                setUndoLimit();
            }
            else
                MAX_UNDOS = undos;
        }
        catch(NumberFormatException e)
        {
            JOptionPane.showMessageDialog(null, "A number (integer) is required.");
            setUndoLimit();
        }
    }  

    //Get the highest up object at the coordinates that the user clicked
    public MapEntity getHighestDepthObject(int mouseX, int mouseY)
    {
        MapEntity ent = null;
        Iterator iter = mapImageList.iterator();
        while(iter.hasNext())
        {
            MapEntity temp = (MapEntity)iter.next();
            if(temp.getHitBox().contains(mouseX, mouseY))
            {
                ent = temp;
            }
        }
        return ent;
    }

    //Print a message with defaults
    public void print(Object message)
    {
        JOptionPane.showMessageDialog(null, message);
    }

    //Print a message with a title and type
    public void print(Object message, String title, int code)
    {
        JOptionPane.showMessageDialog(null, message, title, code);
    }

    private void createSpawnPoint() 
    {     
        minimizeBrowser();
        MapEntity entity = new SpawnEntity(null, 0, 0, 0, 0);
        setOnCursor(entity);
    }

    //This class displays everything to the screen and handles all key and mouse input
    public class Display extends JComponent implements MouseInputListener, KeyListener
    {
        private Editor e;
        private boolean keyData[] = new boolean[256];
        private MapEntity movedObject = null;
        Logger log = new Logger("Key & Mouse Listener", 30, 30);
        CollideZone zone = null;

        /*
         *  0 - true: mouse button 1 pressed
         *  1 - true: mouse button 3 pressed
         */
        private boolean mouseData[] = new boolean[10];
                
        public Display(Editor ed)
        {
            e = ed;
            setFocusable(true);
            addKeyListener(this);
            addMouseMotionListener(this);
            addMouseListener(this);
            //log.showLog();
        }

        @Override
        public void paintComponent(Graphics g)
        {
            Graphics2D g2 = (Graphics2D)g;
            
            g2.scale(currentZoomAmount, currentZoomAmount);

            //Draws all images (adjusted depending on shift)
            for (MapEntity object : mapImageList)
            {
                Image tempImage = imageMap.get(object.getImageName());
                g2.drawImage(tempImage, object.getX() - shiftedX, object.getY() - shiftedY, e);
            }

            //Draws the object on the cursor
            if (onCursor != null)
            {
                if(movingObject)
                {
                    g2.setColor(Color.WHITE);
                    g2.fill3DRect((int)movedObject.getHitBox().x - shiftedX, (int)movedObject.getHitBox().y - shiftedY,
                            (int)movedObject.getHitBox().width, (int)movedObject.getHitBox().height, true);
                }
                g2.drawImage(imageMap.get(onCursor.getImageName()), snap(mouseX), snap(mouseY), e);
            }

            //Draws collide zones
            if(zone != null)
            {
                g2.drawRect(zone.getX(), zone.getY(), zone.getWidth(), zone.getHeight());
            }

            //If hitboxes are enabled
            if(hitboxStatus)
            {
                for(MapEntity object : mapImageList)
                {
                    g2.setColor(Color.GREEN);
                    //Draws the hitbox for all objects in the image list, depending on mode
                    if(hitboxMode == 0)
                        g2.drawRect((int)object.getHitBox().x - shiftedX,(int)object.getHitBox().y - shiftedY,
                            (int)object.getHitBox().width, (int)object.getHitBox().height);
                    else if(hitboxMode == 1)
                        g2.fillRect((int)object.getHitBox().x - shiftedX,(int)object.getHitBox().y - shiftedY,
                            (int)object.getHitBox().width, (int)object.getHitBox().height);
                }
                
                //Draws hitbox for the current object on the cursor, depending on mode
                /* THROWING NULL POINTER
                if(onCursor != null)
                {
                    if(hitboxMode == 0)
                        g2.drawRect((int)onCursor.getHitBox().x - shiftedX,(int)onCursor.getHitBox().y - shiftedY,
                            (int)onCursor.getHitBox().width, (int)onCursor.getHitBox().height);
                    else if(hitboxMode == 1)
                        g2.fillRect((int)onCursor.getHitBox().x - shiftedX,(int)onCursor.getHitBox().y - shiftedY,
                            (int)onCursor.getHitBox().width, (int)onCursor.getHitBox().height);
                }
                */
            }
            
            //Generates on or off string based on hitboxStatus
            String hitboxModeString;
            if(hitboxStatus)
                hitboxModeString = "On";
            else
                hitboxModeString = "Off";

            //Generates frame or fill string based on hitboxMode
            String hitboxModeString2;
            if(hitboxMode == 0)
                hitboxModeString2 = "Frame";
            else
                hitboxModeString2 = "Fill";
            
                  
            //If grid is enabled
            if(gridStatus)
            {              
                g2.setColor(Color.GREEN);
                for(int i = 0; i < (MAP_WIDTH/GRID_SNAP); i++)
                    g2.drawRect(0 + (i*GRID_SNAP), 0, GRID_SNAP, MAP_HEIGHT);
                for(int i = 0; i < (MAP_HEIGHT/GRID_SNAP); i++)
                    g2.drawRect(0, 0 + (i*GRID_SNAP), MAP_WIDTH, GRID_SNAP);    
            }
                       

            //scale concatenates to the current zoom, so invert back to the original drawing to draw the rectangle, so it doesn't change scale
            g2.scale((1/currentZoomAmount), (1/currentZoomAmount));
            
            //Draws grey rectangle to hold info
            g2.setColor(Color.LIGHT_GRAY);
            g2.fillRect(MAP_WIDTH-(MAP_WIDTH/10),0,MAP_WIDTH/10, MAP_HEIGHT/6);

            //Sets up string to display current map the user is working on
            String mapString;
            if(fileL == null)
                mapString = "Untitled";
            else
            {
                mapString = "" + fileL.getName();
                if(mapString.length() > 17)
                    mapString = mapString.substring(0, 15) + "...";
            }

            //Draws info as text in rectangle
            g2.setColor(Color.BLACK);
            g2.drawString("Map: " + mapString, MAP_WIDTH-(int)(MAP_WIDTH/10.5), 15);
            g2.drawString("Mouse X: " + mouseX, MAP_WIDTH-(int)(MAP_WIDTH/10.5), 30);
            g2.drawString("Mouse Y: " + mouseY, MAP_WIDTH-(int)(MAP_WIDTH/10.5), 45);
            g2.drawString("Shifted X: " + shiftedX, MAP_WIDTH-(int)(MAP_WIDTH/10.5), 60);
            g2.drawString("Shifted Y: " + shiftedY, MAP_WIDTH-(int)(MAP_WIDTH/10.5), 75);
            g2.drawString("Hitbox Status: " + hitboxModeString, MAP_WIDTH-(int)(MAP_WIDTH/10.5), 90);
            g2.drawString("Hitbox Mode: " + hitboxModeString2, MAP_WIDTH-(int)(MAP_WIDTH/10.5), 105);
            g2.drawString("Grid Snap: " + GRID_SNAP, MAP_WIDTH-(int)(MAP_WIDTH/10.5), 120);
            
            //miniMap.update(g2, mapImageList, imageMap);
        }

        public void mouseClicked(MouseEvent e){}
        public void mouseEntered(MouseEvent e){}
        public void mouseExited(MouseEvent e){}
        public void mousePressed(MouseEvent e)
        {
            mouseX = (int)(e.getX() / currentZoomAmount);
            mouseY = (int)(e.getY() / currentZoomAmount);

            int button = e.getButton();
            //Left click
            if (button == 1)
            {
                if (onCursor != null) //user has object selected and on mouse
                {
                    //If stickyMoving is enabled, or if the user's object on the mouse is one that was selected from the browser
                    if(stickyMoving || (!stickyMoving && !movingObject))
                    {
                        //Point "reset" to the object on the cursor
                        MapEntity reset = onCursor;

                        //Add what's on the cursor to the map and add it to the linked list
                        onCursor.setX(snap(mouseX) + shiftedX);
                        onCursor.setY(snap(mouseY) + shiftedY);
                        onCursor.setHitBox();
                        mapImageList.add(onCursor);
                        
                        if(movingObject)
                        {
                            addToActions(new undoNode(movedObject, onCursor, new DynamicLocation(movedObject.getX(), movedObject.getY(), onCursor.getX(), onCursor.getY()),
                                undoNode.MOVE, mapImageList.indexOf(movedObject)));
                        }
                        else
                        {
                            addToActions(new undoNode(onCursor, new Location(onCursor.getX(), onCursor.getY()),
                                undoNode.ADD, mapImageList.indexOf(movedObject)));
                        }
                            
                        //Puts the same object on the cursor
                        if(reset instanceof BackgroundEntity)
                            onCursor = new BackgroundEntity((BackgroundEntity)reset);
                        if(reset instanceof CollidableEntity)
                            onCursor = new CollidableEntity((CollidableEntity)reset);
                    }
                    //If stickyMoving is off (this will only be accessed when movingObject is true)
                    else if(!stickyMoving)
                    {
                        setLastKnownEntity(onCursor);
                        
                        //Adds what's on the cursor to the map and add it to the linked list
                        onCursor.setX(snap(mouseX) + shiftedX);
                        onCursor.setY(snap(mouseY) + shiftedY);
                        onCursor.setHitBox();
                        mapImageList.add(onCursor);

                        addToActions(new undoNode(movedObject, onCursor, new DynamicLocation(movedObject.getX(), movedObject.getY(), onCursor.getX(), onCursor.getY()),
                            undoNode.MOVE, mapImageList.indexOf(movedObject)));

                        //Removes the object from the cursor
                        onCursor = null;
                    }

                    //If user is moving an object
                    if(movingObject)
                    {
                        //Removes the original (the moved) object from the linked list. The object that was put on the cursor will
                        //be added to the map and linked list in the if statements above. This is preserve the "depth" of the object
                        mapImageList.remove(movedObject);
                        movingObject = false;
                    }

                    hasSaved = false;
                    menuItem_edit_undo.setEnabled(true);

                    //If the user has things "undone," it will reset
                    if(!redoStack.isEmpty())
                        resetUndos();
                }
                //If shift is pressed
                else if(keyData[16])
                {
                    mouseData[0] = true;
                    log.addLine("shift + left mouse pressed");
                }
                else
                {
                    //If there is no object on the mouse, checks to see if the user is clicking on a placed object
                    movedObject = getHighestDepthObject(mouseX + shiftedX, mouseY + shiftedY);
                    if(movedObject != null)
                    {
                        if(movedObject instanceof BackgroundEntity)
                            onCursor = new BackgroundEntity((BackgroundEntity)movedObject);
                        else if(movedObject instanceof CollidableEntity)
                            onCursor = new CollidableEntity((CollidableEntity)movedObject);

                        movingObject = true;
                    }
                }
                
            }
            //Right click
            else if(button == 3)
            {
                //If user has object selected and on mouse, take it off mouse
                if(onCursor != null)
                {
                    setLastKnownEntity(onCursor);
                    onCursor = null;
                    movingObject = false;
                    movedObject = null;
                }
                //If user is right clicking on map with no object on the mouse
                else
                {
                    MapEntity object = getHighestDepthObject(mouseX + shiftedX, mouseY + shiftedY);

                    //If the user right clicked on an object on the map (so it gets deleted)
                    if(object != null)
                    {
                        addToActions(new undoNode(object, new Location(object.getX(), object.getY()),
                            undoNode.DELETE, mapImageList.indexOf(object)));

                        mapImageList.remove(object);
                        hasSaved = false;
                        menuItem_edit_undo.setEnabled(true);

                        //If the user has used undo, it resets it
                        if(!redoStack.isEmpty())
                        {
                            resetUndos();
                        }         
                    }
                }
            }
            repaint();
        }
        public void	mouseReleased(MouseEvent e)
        {
            mouseData[0] = false;
        }
        public void mouseDragged(MouseEvent e)
        {
            mouseX = (int)(e.getX() / currentZoomAmount);
            mouseY = (int)(e.getY() / currentZoomAmount);

            //Mouse button 1 pressed
            if(mouseData[0]) 
            {
                //Shift
                if(keyData[16]) 
                {
                    if(zone == null)
                    {
                        zone = new CollideZone("New", mouseX, mouseY, 0, 0);
                    }
                    else
                    {
                        zone.setWidth(mouseX - zone.getX());
                        zone.setHeight(mouseY - zone.getY());
                    }
                    log.addLine("shift + left mouse pressed + mouse drag (" + mouseX + ", " + mouseY + ")");
                }
            }

            repaint();
        }
        public void mouseMoved(MouseEvent e)
        {
            mouseX = (int)(e.getX() / currentZoomAmount);
            mouseY = (int)(e.getY() / currentZoomAmount);
            if (onCursor != null)
            {
                onCursor.setX(snap(mouseX));
                onCursor.setY(snap(mouseY));
            }
    
            repaint();
        }
        public void keyPressed(KeyEvent e)
        {
            char c = e.getKeyChar();
            int i = e.getKeyCode();

            if(i == 16) //shift
            {
                //If shift is not pressed already (stop spamming the log)
                if(!keyData[16])
                    log.addLine("shift pressed");
                
                keyData[16] = true;  
            }
            if(i == 17) //control
                keyData[17] = true;
            if(c == 'z')
            {
                //if(keyData[0] == true) //control
                   // undo();
                //JOptionPane.showMessageDialog(null, e.getKeyCode());
            }
            if(c == 'y')
            {
                //if(keyData[0] == true) //control
                    //redo();
                //JOptionPane.showMessageDialog(null, e.getKeyCode());
            }
        }

        public void keyReleased(KeyEvent e)
        {
            if(e.getKeyCode() == 17) //control
                keyData[17] = false;
            else if(e.getKeyCode() == 16) //shift
            {
                keyData[16] = false;
                log.addLine("shift released");
            }    
        }

        @SuppressWarnings("empty-statement")
        public void keyTyped(KeyEvent e)
        {
            //0: left control
            char c = e.getKeyChar();
            if(c == 'w')
            {
                shiftedY -= GRID_SNAP * 2;
            }
            else if(c == 'a')
            {
                shiftedX -= GRID_SNAP * 2;
            }
            else if(c == 'd')
            {
                shiftedX += GRID_SNAP * 2;
            }
            else if(c == 's')
            {
                shiftedY += GRID_SNAP * 2;
            }
            else if(c == 'u')
            {
                while(dM.debug());
            }
            else if(c == 'h')
            {
                //getSomeImages();
            }
            repaint();
        }

        public int snap(int loc)
        {
            return (loc / GRID_SNAP * GRID_SNAP);
        }
    }

    public class DebugMenu
    {
        int debug;
        public DebugMenu()
        {
            debug = 0;
        }
        public boolean debug()
        {
            String input = JOptionPane.showInputDialog("Debug Menu\nLast updated: 6/10/09\n\n1. Booleans\n2. Strings\n3. Ints/Doubles\n4. Others\n\n");
            if(input == null)
                return false;
            else if(input.equals(""))
                return true;
            else if(Utility.IsNumericalString(input))
                debug = Integer.parseInt(input);
            else if(input.equals("exit"))
                return false;
            else
                return true;
            
            switch(debug)
            {
                case 1: booleans(); return true;
                case 2: strings(); return true;
                case 3: numbers(); return true;
                case 4: others(); return true;
            }
            return true;
        }

        private void booleans()
        {
            while(true)
            {
                debug = 0;
                String input = JOptionPane.showInputDialog("Booleans\n\n1. hasSaved\n3. usingNewMap\n\n");
                if(input == null)
                    break;
                else if(Utility.IsNumericalString(input))
                {
                    debug = Integer.parseInt(input);
                }

                switch(debug)
                {
                    case 1: show(hasSaved); break;
                    case 2: show(usingNewMap); break;
                }
            }
        }

        private void numbers()
        {
            while(true)
            {
                debug = 0;
                String input = JOptionPane.showInputDialog("Numbers\n\n1. MAX_UNDOS\n2. mouseX\n3. mouseY\n4. shiftedX\n5. shiftedY\n6. undoCount\n7. debug\n8. GRID_SNAP\n\n");
                if(input == null)
                    break;
                else if(Utility.IsNumericalString(input))
                {
                    debug = Integer.parseInt(input);
                }

                switch(debug)
                {
                    case 1: show(MAX_UNDOS); break;
                    case 2: show(mouseX); break;
                    case 3: show(mouseY); break;
                    case 4: show(shiftedX); break;
                    case 5: show(shiftedY); break;
                    case 6: show(undoCount); break;
                    case 7: show(this.debug); break;
                    case 8: show(GRID_SNAP); break;
                }
            }
        }

        private void strings()
        {
            while(true)
            {
                debug = 0;
                String input = JOptionPane.showInputDialog("Strings\n\n1. IMAGES_ADDRESS\n2. MAP_FILE_EXTENSION\n\n");
                if(input == null)
                    break;
                else if(Utility.IsNumericalString(input))
                {
                    debug = Integer.parseInt(input);
                }

                switch(debug)
                {
                    case 1: show(MAP_FILE_EXTENSION); break;
                }
            }
        }
        
        private void others()
        {
            while(true)
            {
                debug = 0;
                String input = JOptionPane.showInputDialog("Others\n\n1. fileL\n2. imageMap\n3. mapImageList\n4. onCursor\n5. redoStack\n" +
                        "6. Items in imageMap\n7. Items in mapImageList\n\n");
                if(input == null)
                    break;
                else if(Utility.IsNumericalString(input))
                {
                    debug = Integer.parseInt(input);
                }

                switch(debug)
                {
                    case 1: show(fileL); break;
                    case 2: show(imageMap); break;
                    case 3: show(mapImageList); break;
                    case 4: show(onCursor); break;
                    case 5: show(redoStack); break;
                    case 6: show(imageMap.size()); break;
                    case 7: show(mapImageList.size()); break;
                }
            }
        }

        private void show(Object variable)
        {
            JOptionPane.showMessageDialog(null, variable);
        }
    }
}