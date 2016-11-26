package tools;

import java.io.File;
import javax.swing.filechooser.FileSystemView;

public class Utility
{
    public static String primaryDrive = null;

    //return an int between the min and max
    public static int RandomInt(int min, int max)
	{
		return (((int)(Math.random() * (max - min + 1))) + min);
	}

    //test to see if a string is a number. true if it is, else false
    public static boolean IsNumericalString(String string)
    {
        try
        {
            Integer.parseInt(string);
            return true;
        }
        catch(NumberFormatException e)
        {
            return false;
        }
    }

    //round a double to the specified number of places
    public static double RoundToDecimalPlace(double number, int places)
    {
        double scalar = Math.pow(10.0, places);
        int scaled = (int)(number * scalar);
        double number2 = ((double)scaled)/scalar;
        return number2;
    }

    //find the current disk drive the user is on
    public static boolean FindPrimaryDrive()
    {
        boolean match = false;
        FileSystemView fsv = FileSystemView.getFileSystemView();
        String drive = fsv.getHomeDirectory().getPath().substring(0, 3);

        File[] roots = File.listRoots();

        for(int index = 0; index < roots.length; index++)
        {
           String root = roots[index].toString();
           if(root.equals(drive))
           {
               match = true;
               break;
           }
        }

        if(match)
            primaryDrive = drive;

        return match;
    }

    //get a file's extension by checking the string
    public static String GetExtension(String file)
    {
        return file.substring(file.lastIndexOf(".") + 1);
    }
}
