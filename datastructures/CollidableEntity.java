package datastructures;

import java.awt.geom.Rectangle2D;

public class CollidableEntity extends MapEntity
{
    public String imageName;
    protected int x;
    protected int y;
    protected int width;
    protected int height;
    private Rectangle2D.Double hitBox;

    public CollidableEntity(String name, int objectX, int objectY, int objectWidth, int objectHeight)
    {
        imageName = name;
        x = objectX;
        y = objectY;
        width = objectWidth;
        height = objectHeight;
        hitBox = new Rectangle2D.Double(x, y, width, height);
    }

    //used to reset a backgroundentity. This is used as opposed to referencing the previous object.
    public CollidableEntity(CollidableEntity mo)
    {
        this.x = mo.x;
        this.y = mo.y;
        this.width = mo.width;
        this.height = mo.height;
        this.imageName = mo.imageName;

        setHitBox();
    }

    public int getX()
    {
        return x;
    }

    public int getY()
    {
        return y;
    }

    public Rectangle2D.Double getHitBox()
    {
        return hitBox;
    }

    public void setHitBox()
    {
        hitBox = new Rectangle2D.Double(x, y, width, height);
    }

    public void setX(int objectX)
    {
        x = objectX;
    }

    public void setY(int objectY)
    {
        y = objectY;
    }

    public String getImageName()
    {
        return imageName;
    }

    @Override
    public String toString()
    {
        return (MapEntity.EntityType.COLLIDABLE.ordinal() + " " + imageName + " " + (int)x + " " + (int)y + " ");
    }
}
