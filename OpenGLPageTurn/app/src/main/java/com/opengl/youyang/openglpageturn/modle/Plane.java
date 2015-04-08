package com.opengl.youyang.openglpageturn.modle;

/**
 * 类描述：
 * 
 * @Package com.yy.muyu.view
 * @ClassName: Plane
 * @author 尤洋
 * @mail youyang@ucweb.com
 * @date 2015-3-14 下午8:13:05
 */
public class Plane extends Mesh {

    /**
     * 构造方法描述：
     * 
     * @Title: Plane
     * @date 2015-3-14 下午8:13:05
     */
    public Plane() {
        this(1, 1, 1, 1);
    }

    public Plane(float width, float height) {
        this(width, height, 1, 1);
    }

    /**
     * 
     * 构造方法描述：
     * @Title: Plane
     * @param width   宽度
     * @param height  高度
     * @param widthSegments 宽度可以分成的份数
     * @param heightSegments高度可以分成的份数
     * @date 2015-3-15 下午8:57:04
     */
    public Plane(float width, float height, int widthSegments, int heightSegments) {
        float[] vertices = new float[
                (widthSegments + 1) * (heightSegments + 1) * 3];
        short[] indices = new short[
                ((widthSegments + 1) * (heightSegments + 1) * 6)];
        float xOffset = width / -2;  //x上起始偏移量
        float yOffset = height / -2; //y轴上的起始偏移量 
        float xWidth = width / (widthSegments);  //每一份segment的宽度
        float yHeight = height / (heightSegments); //每一份segment的高度
        int currentVertex = 0;
        int currentIndex = 0;
        short w = (short) (widthSegments + 1);//一条x轴上的顶点数
        for (int y = 0; y < heightSegments + 1; y++) {
            for (int x = 0; x < widthSegments + 1; x++) {
                // 初始化顶点坐标
                vertices[currentVertex] = xOffset + x * xWidth;//x坐标
                vertices[currentVertex + 1] = yOffset + y * yHeight;//y坐标
                vertices[currentVertex + 2] = 0; //z坐标
                currentVertex += 3;
                int n = y * (widthSegments + 1) + x; //第几个顶点
                
                if(y<heightSegments&&x<widthSegments){
                    //俩个三角形构成一个正方形
                    /**
                     *      1234    顶点排序
                     *      5678        
                     */
                    //第一个三角形  125
                    indices[currentIndex]=(short)n;
                    indices[currentIndex+1]=(short)(n+1);
                    indices[currentIndex+2]=(short)(n+w);
                    //第二个三角形  265
                    indices[currentIndex+3]=(short)(n+1);
                    indices[currentIndex+4]=(short)(n+1+w);
                    indices[currentIndex+5]=(short)(n+1+w-1);
                    
                    currentIndex+=6;    
                }
                
            }

        }
        setIndeices(indices);
        setVerteices(vertices);

    }

}
