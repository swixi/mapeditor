package datastructures;

public class CollideZone extends CollidableEntity
{
    public CollideZone(String zoneName, int zoneX, int zoneY, int zoneWidth, int zoneHeight)
    {
        super(zoneName, zoneX, zoneY, zoneWidth, zoneHeight);
    }

    public void setWidth(int zoneWidth)
    {
        width = zoneWidth;
    }

    public void setHeight(int zoneHeight)
    {
        height = zoneHeight;
    }

    public int getWidth()
    {
        return width;
    }

    public int getHeight()
    {
        return height;
    }

    @Override
    public String toString()
    {
        return "CollideZone " + imageName + " " + (int)x + " " + (int)y + " " + (int)width + " " + (int)height + " ";
    }
}
