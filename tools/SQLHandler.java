package tools;

import datastructures.SQLMapEntity;
import java.sql.*;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.TreeMap;

public class SQLHandler
{
    private Connection con;
    private String url;
    private String db;
    private String driver;
    private String user;
    private String pass;
    private String staticMap;
    private String imageLocations;

    public SQLHandler() throws Exception
    {
        con = null;
        url = "***REMOVED***";
        db = "QuarantineDB";
        driver = "sun.jdbc.odbc.JdbcOdbcDriver";
        user = "***REMOVED***";
        pass = "***REMOVED***";
        staticMap = "staticmap";
        imageLocations = "imagelocations";

        Class.forName(driver).newInstance();
        con = DriverManager.getConnection(url+db, user, pass);
    }

    //used for retrieving data
    private ResultSet createSQLCommand(String command)
    {
        try
        {
            Statement st = con.createStatement();
            ResultSet res = st.executeQuery(command);
            return res;
        }
        catch (SQLException s)
        {
            s.printStackTrace();
        }
        return null;
    }

    //used for manipulating data
    private void createSQLUpdate(String command)
    {
        try
        {
            Statement st = con.createStatement();
            st.executeUpdate(command);
        }
        catch (SQLException s)
        {
            s.printStackTrace();
        }
    }

    //load all of the images in the map, adding them to a LL of sqlMapEntities
    public LinkedList getStaticObjects()
    {
        LinkedList objectList = new LinkedList();

        ResultSet r = createSQLCommand("SELECT * FROM " + staticMap + ";");
        try
        {
            while(r.next())
            {
                objectList.add(new SQLMapEntity(r.getInt("id"), r.getInt("x"), r.getInt("y")));
            }
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }

        return objectList;
    }

    //load the imagelocations query and make a map to locate images, using Integers (ids) to point to Strings (names of images)
    public Map getObjectMap()
    {
        Map objectMap = new HashMap<Integer, String>();

        ResultSet r = createSQLCommand("SELECT * FROM " + imageLocations + ";");
        try
        {
            while(r.next())
            {
                objectMap.put(r.getInt("id"), r.getString("name"));
            }
            return objectMap;
        }
        catch(Exception e)
        {
            e.printStackTrace();
            return null;
        }
    }

    public TreeMap getOrderedObjectMap()
    {
        TreeMap objectMap = new TreeMap<Integer, String>();

        ResultSet r = createSQLCommand("SELECT * FROM " + imageLocations + ";");
        try
        {
            while(r.next())
            {
                objectMap.put(r.getInt("id"), r.getString("name"));
            }
            return objectMap;
        }
        catch(Exception e)
        {
            e.printStackTrace();
            return null;
        }
    }

    //gives the objectMap in reverse order for id lookup
    public Map getReverseObjectMap()
    {
        Map objectMap = new HashMap<String, Integer>();

        ResultSet r = createSQLCommand("SELECT * FROM " + imageLocations + ";");
        try
        {
            while(r.next())
            {
                objectMap.put(r.getString("name"), r.getInt("id"));
            }
            return objectMap;
        }
        catch(Exception e)
        {
            e.printStackTrace();
            return null;
        }
    }

    //deletes all data from the map
    public void clearCurrentMap()
    {
        createSQLUpdate("DELETE FROM " + staticMap + ";");
    }

    //inserts objects into the map
    public void insertIntoMap(int id, int x, int y)
    {
        createSQLUpdate("insert into " + staticMap + " ( id, x, y ) values (" + id + ", " + x + ", " + y + ");");
    }

    public void insertIntoDatabase(int id, String location, String name)
    {
        createSQLUpdate("insert into " + imageLocations + " ( id, location, name ) values (" + id + ", '" + location + "', '" + name + "');");
    }

    //closes the connection
    public void close()
    {
        try
        {
            con.close();
        }
        catch(Exception e)
        {
            e.printStackTrace();
        } 
    }
}