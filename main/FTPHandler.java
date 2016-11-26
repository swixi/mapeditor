package main;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import javax.imageio.ImageIO;
import javax.swing.JOptionPane;
import objectbrowser.*;
import org.apache.commons.net.ftp.*;
import tools.*;

public class FTPHandler
{
    private Editor editor;

    protected final String IMAGES_ADDRESS = "***REMOVED***";
    
    protected FTPClient ftpC;
    protected URL imageURL;
    private ObjectBrowser objectBrowser;
    private SQLHandler sql;
    protected static Logger sqlLogger;
    protected static Logger imageLogger;
     
    protected int numImageCategories = 0;

    private String HOST_NAME;
    protected String username = "***REMOVED***";
    protected String password = "***REMOVED***";
    protected String imageWorkingDirectory = "/edit/objects";
    protected String mapWorkingDirectory = "/edit/maps";
    protected String editWorkingDirectory = "/edit";

    public FTPHandler(Editor ed)
    {
        editor = ed;
        HOST_NAME = editor.HOST_NAME; 
        sql = editor.sqlH;
    }

    //connects to the ftp server and fetches all of the map images, and places them in the objectBrowser
    //returns true if connected; false if an error occurs
    public boolean loadImages(boolean updateSQLImages, boolean updateImagesOnDisk)
    {
        String IMAGE_PATH = editor.IMAGE_PATH;
        String currentLocalDirectory = IMAGE_PATH;
        String[] filesInCurrentLocalDirectory = null;
        ArrayList<String> filesInCurrentRemoteDirectory = new ArrayList<String>();
        ArrayList<String> imageCategoryFolders = new ArrayList<String>();
        objectBrowser = editor.objectBrowser;
        File imageDirectory = null;

        //reset all data
        int panelNumber = 0;
        numImageCategories = 0;
        long time1 = 0;

        //SQL data
        int sqlIndex = -100;
        int initialSQLIndex = 0;
        Map objectMap = new HashMap();
        TreeMap orderedMap = new TreeMap();

        ftpC = new FTPClient();

        try
        {
            //if the user wants to update the sql table
            if(updateSQLImages)
            {
                sql = new SQLHandler();
                objectMap = sql.getReverseObjectMap();
                orderedMap = sql.getOrderedObjectMap();

                if(orderedMap.size() == 0)
                    sqlIndex = 1;
                else
                    sqlIndex = ((Integer)orderedMap.lastKey()).intValue() + 1;
                initialSQLIndex = sqlIndex;
                sqlLogger = new Logger("Updated Data", 20, 40);
                sqlLogger.showLog();
                sqlLogger.addLine("The following changes happened to the SQL table:\n");
            }
        }
        catch (Exception ex)
        {
            editor.print("Error with the SQL connection while loading FTP. Exception: " + ex.getMessage());
        }

        try
        {
            //create a logger to tell user that the program is downloading the images
            imageLogger = new Logger("Downloading", 30, 36);
            imageLogger.showLog();
            imageLogger.setToCenter();

            if(updateImagesOnDisk)
                imageLogger.addLine("1 - LOCAL UPDATE MODE\n");
            if(updateSQLImages)
                imageLogger.addLine("2 - SQL UPDATE MODE\n");
            if(!updateImagesOnDisk)
                imageLogger.addLine("3 - REMOTE MODE\n");

            ftpC.connect(HOST_NAME);
            imageLogger.addLine("Connected to " + HOST_NAME + ". \n" + ftpC.getReplyString());

            //try to login to the ftp address with the specified username/password
            if(!ftpC.login(username, password))
            {
                throw new Exception("Unable to login to FTP server using selected username and password.");
            }

            ftpC.changeWorkingDirectory(imageWorkingDirectory);
            ftpC.setFileType(FTP.BINARY_FILE_TYPE);

            imageLogger.addLine("Downloading images and building browser...\n");

            if(updateImagesOnDisk)
                imageLogger.addLine("Updating directory: " + IMAGE_PATH + "\n");

            //Gets an array of the directories (image categories) at the image address
            FTPFile files[] = ftpC.listFiles();

            time1 = System.nanoTime();

            //Loops through the category folders
            for(int i = 0; i < files.length; i++)
            {
                //Gets a folder (image category)
                FTPFile imageFolder = files[i];

                //If the folder is an actual folder
                if(!(imageFolder.getName().equals(".") || imageFolder.getName().equals("..")))
                {
                    imageCategoryFolders.add(imageFolder.getName());

                    if(updateImagesOnDisk)
                    {
                        filesInCurrentRemoteDirectory = new ArrayList<String>();
                        imageDirectory = new File(IMAGE_PATH + "\\" + imageFolder.getName());
                        if(!imageDirectory.exists())
                        {
                            imageDirectory.mkdir();
                            imageLogger.addLine("CREATED -> " + imageDirectory.toString() + "\n");
                        }
                        currentLocalDirectory = imageDirectory.toString();
                        filesInCurrentLocalDirectory = imageDirectory.list();
                    }

                    //Used for testing purposes only. Change 999 to whatever number categories you want to stop after.
                    if(numImageCategories == 999)
                        throw new Exception("Stop loading");
                    
                    numImageCategories++;
                    objectBrowser.addTab(imageFolder.getName());
                    ftpC.changeWorkingDirectory(ftpC.printWorkingDirectory() + "/" + imageFolder.getName());

                    //loop through the images in the folder
                    for(FTPFile image : ftpC.listFiles())
                    {
                        //if it is an actual image
                        if(!(image.getName().equals(".") || image.getName().equals("..")))
                        {
                            try
                            {
                                long time3 = System.nanoTime();
                                imageURL = new URL(IMAGES_ADDRESS + imageFolder.getName() + "/" + image.getName());
                                if(updateSQLImages)
                                {
                                    //if the current image is not in the table, add it and print a message to the user
                                    if(!objectMap.containsKey(image.getName()))
                                    {
                                        sql.insertIntoDatabase(sqlIndex, imageURL.toString(), image.getName());
                                        sqlLogger.addLine("ADDED ->\nid: " + sqlIndex + "\nlocation: " + imageURL.toString() + "\nname: " + image.getName() + "\n");
                                        sqlIndex++;
                                    }
                                }
                                BufferedImage currentImage = ImageIO.read(imageURL);
                                //Image currentImage = getImage(new URL(IMAGES_ADDRESS + imageFolder.getName() + "/" + image.getName())); //non-buffered image

                                //update data for logger
                                imageLogger.addLine("LOADED -> " + imageFolder.getName() + "\\" + image.getName() + " (" + Utility.RoundToDecimalPlace(
                                    ((System.nanoTime() - time3)/1000000000.0), 3) + " seconds)");
                                imageLogger.updateTitle("Downloading (" + Utility.RoundToDecimalPlace(((System.nanoTime() - time1)/1000000000.0), 3)
                                    + " seconds)");

                                if(updateImagesOnDisk)
                                {
                                    boolean fileExists = false;
                                    for(String file : filesInCurrentLocalDirectory)
                                    {
                                        if(file.equals(image.getName()))
                                            fileExists = true;
                                    }
                                    
                                    if(!fileExists)
                                    {
                                        //creates new file at outputFileName location
                                        File file = new File(currentLocalDirectory + "\\" + image.getName());
                                        file.createNewFile();

                                        //downloads file to File f's location
                                        boolean retValue = ftpC.retrieveFile(image.getName(), new FileOutputStream(file));
                                        if (!retValue)
                                        {
                                            throw new Exception ("Could not download " + file);
                                        }
                                        imageLogger.addLine("WROTE TO FILE\n");
                                    }
                                    else
                                        imageLogger.addLine("UP TO DATE\n");
                                }

                                //add the image to the map, object browser, and arraylist
                                editor.imageMap.put(image.getName(), currentImage);
                                objectBrowser.addImage(panelNumber, currentImage, image.getName());
                                filesInCurrentRemoteDirectory.add(image.getName());
                            }
                            catch(Exception e)
                            {
                                editor.print("Image " + image.getName() + " could not be loaded. Exception: " + e);
                                e.printStackTrace();
                            }
                        }
                    }

                    if(updateImagesOnDisk)
                    {
                        //get the list of files in the current local directory after updating
                        filesInCurrentLocalDirectory = imageDirectory.list();

                        //for each file in the local directory (image category), check it against the files from the remote directory (at the same image category)
                        for(String file : filesInCurrentLocalDirectory)
                        {
                            //if the remote directory doesn't contain this file, and the extension is not "old", then it is a file that doesn't belong
                            if(!filesInCurrentRemoteDirectory.contains(file) && !Utility.GetExtension(file).equals("old") && !file.equals("Thumbs.db"))
                            {
                                //call deleteFile() to either rename or delete the bad file
                                File badFile = new File(imageDirectory + "\\" + file);
                                File renamedFile = new File(imageDirectory + "\\" + file + ".old");
                                deleteFile(badFile, renamedFile, imageFolder.getName() + "\\");
                            }
                        }
                    }
                    ftpC.changeWorkingDirectory("..");
                    objectBrowser.paintTab(panelNumber);
                    panelNumber++;
                }
            }

            if(updateImagesOnDisk)
            {
                //Get a list of the folders (and files) at IMAGE_PATH
                File imagePathDirectory = new File(IMAGE_PATH);     
                String[] filesInImagePath = imagePathDirectory.list();

                for(String file : filesInImagePath)
                {
                    //if the remote directory doesn't contain this file, and the extension is not "old", then it is a file that doesn't belong
                    if(!imageCategoryFolders.contains(file) && !Utility.GetExtension(file).equals("old") && !file.equals("Thumbs.db"))
                    {
                        //call deleteFile() to either rename or delete the bad file
                        File badFile = new File(imagePathDirectory + "\\" + file);
                        File renamedFile = new File(imagePathDirectory + "\\" + file + ".old");
                        deleteFile(badFile, renamedFile, "\\");
                    }
                }
            }
            editor.menuBar.validate();
            
            if(initialSQLIndex == sqlIndex)
                sqlLogger.addLine("No changes were made.");
            if(updateImagesOnDisk)
                imageLogger.addLine("\nUpdating complete!");
            else
                imageLogger.addLine("\nImage loading complete!");
            
            imageLogger.updateTitle("Downloading completed in " + Utility.RoundToDecimalPlace(((System.nanoTime() - time1)/1000000000.0), 2) + " seconds.");
        }
        catch(Exception e)
        {          
            imageLogger.addLine("\nAn error occured while downloading:" + "\n" + e.toString());
            imageLogger.updateTitle("Downloading terminated after " + Utility.RoundToDecimalPlace(((System.nanoTime() - time1)/1000000000.0), 2) + " seconds.");
            editor.print("Problem with the FTP connection: " + e);
            e.printStackTrace();
        }
        return true;
    }

    //simply reconnects to the ftp server. doesn't retrieve messages or images
    //used for when uploading map to server, or when the connection fades
    public boolean reloadConnection() throws Exception
    {
        //attempt to connect to host and login with un/pw
        ftpC.connect(HOST_NAME);
        if(!ftpC.login(username, password))
        {
            throw new Exception("Unable to login to FTP server using selected username and password.");
        }
        ftpC.changeWorkingDirectory(imageWorkingDirectory);
        ftpC.setFileType(FTP.BINARY_FILE_TYPE);

        return true;
    }

    public void disconnect() throws Exception
    {
        ftpC.disconnect();
    }

    public boolean isConnected()
    {
        return ftpC.isConnected();
    }

    public static Logger getSQLLogger()
    {
        return sqlLogger;
    }

    public static Logger getFTPLogger()
    {
        return imageLogger;
    }

    private void deleteFile(File badFile, File renamedFile, String folder)
    {
        if(!renamedFile.exists())
        {
            badFile.renameTo(renamedFile);
            imageLogger.addLine("RENAMED -> " + folder + badFile.getName() + "\n");
        }
        else
        {
            String[] options = {badFile.getName(), renamedFile.getName()};
            int choice = JOptionPane.showOptionDialog(null, "\"" + folder + badFile.getName() + "\"" + " has already been renamed with the '.old' extension.\n" +
                "Please choose which one to PERMANENTLY delete:\n\n(Note: if these are directories with files in them, they will not be deleted.\nPlease move at " +
                    "least one out of the image directory to prevent further conflicts.)", "File Conflict", JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE,
                        null, options , options[0]);
            if(choice == 0)
            {
                badFile.delete();
                imageLogger.addLine("REMOVED -> " + folder + badFile.getName());
                imageLogger.addLine("KEPT -> " + folder + renamedFile.getName() + "\n");
            }
            else
            {
                renamedFile.delete();
                badFile.renameTo(renamedFile);
                imageLogger.addLine("REMOVED -> " + folder + renamedFile.getName());
                imageLogger.addLine("RENAMED -> " + folder + badFile.getName() + "\n");
            }
        }
    }
}
