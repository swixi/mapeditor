package datastructures;

public class Location
{
    int x;
    int y;

    public Location(int xCoord, int yCoord)
    {
        x = xCoord;
        y = yCoord;
    }

    public Location()
    {
        x = 0;
        y = 0;
    }

    public int getX()
    {
        return x;
    }

    public int getY()
    {
        return y;
    }

    public void setX(int xCoord)
    {
        x = xCoord;
    }

    public void setY(int yCoord)
    {
        y = yCoord;
    }
}
