package datastructures;

public class DynamicLocation extends Location
{
    int newX;
    int newY;

    public DynamicLocation(int oldX, int oldY, int x, int y)
    {
        super(oldX, oldY);
        newX = x;
        newY = y;
    }

    public int getOldX()
    {
        return super.getX();
    }

    public int getOldY()
    {
        return super.getY();
    }

    public int getNewX()
    {
        return newX;
    }

    public int getNewY()
    {
        return newY;
    }

    public void setOldX(int oldX)
    {
        super.setX(oldX);
    }

    public void setOldY(int oldY)
    {
        super.setY(oldY);
    }

    public void setNewX(int newXCoord)
    {
        newX = newXCoord;
    }

    public void getNewY(int newYCoord)
    {
        newY = newYCoord;
    }

}
