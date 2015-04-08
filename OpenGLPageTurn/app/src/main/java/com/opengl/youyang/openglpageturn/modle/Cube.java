package com.opengl.youyang.openglpageturn.modle;

/**
 * 类描述：
 * @Package com.yy.muyu.view
 * @ClassName: Cube
 * @author 尤洋
 * @mail youyang@ucweb.com
 * @date 2015-3-15 下午9:34:06
 */
public class Cube extends Mesh {

    /**
     * 构造方法描述：
     * @Title: Cube
     * @date 2015-3-15 下午9:34:06
     */
    public Cube(float width,float height , float depth) {
        width/=2;
        height/=2;
        depth/=2;
        
        //顶点坐标
        float vertices[]=new float[]{
          -width,-height,-depth,     //0   左下后 
          width,-height,-depth,   //1        右下后
          width,height,-depth,//2          右上后
          -width,height,-depth,//3           左上后
          -width,-height,depth,//4           左下前
          width,-height,depth,//5            右下前
          width,height,depth,//6             右上前
          -width,height,depth,//7            左上前
        };    //  7 4  5   5 6  7
        
        float textureCoordinates[] = { 
                0.0f, 0.0f, //    左上
                0.0f, 0.5f, //左下
                0.5f,  0.5f, //  右下
                0.5f, 0.0f, //    右上
        };
        
        //6个面   12个三角形
        short indices[]=new short[]{
          0,4,5,
          0,5,1,
          1,5,6,
          1,6,2,
          2,6,7,
          2,7,3,
          3,7,4,
          3,4,0,
          4,7,6,
          4,6,5,
          3,0,1,
          3,1,2
        };
        short indices1[]=new short[]{
                4,7,6,
                4,6,5,
                
              };
        setIndeices(indices1);
        setIndeices(indices);
        setVerteices(vertices);
        setTextureCoordinates(textureCoordinates);       
    }

}
