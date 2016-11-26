package datastructures;

public class SQLMapEntity
{
    private int imageId;
    private int x;
    private int y;

    public SQLMapEntity(int imageId, int x, int y)
    {
        this.imageId = imageId;
        this.x = x;
        this.y = y;
    }

    public int getImageId()
    {
        return imageId;
    }

    public int getX()
    {
        return x;
    }

    public int getY()
    {
        return y;
    }
}
