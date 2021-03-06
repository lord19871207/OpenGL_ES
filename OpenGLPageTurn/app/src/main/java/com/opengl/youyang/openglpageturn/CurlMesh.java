/*
   Copyright 2013 Harri Smatt

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */

package com.opengl.youyang.openglpageturn;

import android.graphics.PointF;
import android.graphics.RectF;
import android.opengl.GLES20;
import android.opengl.GLUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * 仿真翻页 三角形网格
 */
public class CurlMesh {

	//避免使用new 避免频繁GC
	private Array<Vertex> mArrIntersections;//交点的数组
	/**输出数据的顶点*/
	private Array<Vertex> mArrOutputVertices;
	/**旋转之后的顶点*/
	private Array<Vertex> mArrRotatedVertices;
	private Array<Double> mArrScanLines;
	private Array<ShadowVertex> mArrShadowDropVertices;
	private Array<ShadowVertex> mArrShadowSelfVertices;
	private Array<ShadowVertex> mArrShadowTempVertices;
	private Array<Vertex> mArrTempVertices;

	// 光栅化的缓存
	private FloatBuffer mBufNormals;
	private FloatBuffer mBufShadowPenumbra;
	private FloatBuffer mBufShadowVertices;
	private FloatBuffer mBufTexCoords;
	private FloatBuffer mBufVertices;

	// 保存顶点数量
	private int mCountShadowDrop;
	private int mCountShadowSelf;
	private int mCountVertices;

	// 页面是否折叠
	private boolean mFlipTexture = false;
	// 用于扭曲效果的最大分割线数量
	private int mMaxCurlSplits;
	
	// 页面对象
	private final CurlPage mPage = new CurlPage();

	//四边形边界  0 左上  1左下  2右上  3右下
	private final Vertex[] mRectangle = new Vertex[4];
	// 纹理id 数组
	private int[] mTextureIds = null;

	/**
	 * mesh对象的构造函数
	 * @param maxCurlSplits
	 *            卷曲的最大分割数目. 数值越大 翻页越平滑.需要画更多的多边形
	 */
	public CurlMesh(int maxCurlSplits) {
		// 至少需要分割一次
		mMaxCurlSplits = maxCurlSplits < 1 ? 1 : maxCurlSplits;

		mArrScanLines = new Array<Double>(maxCurlSplits + 2);//（分割数+2）  条线  包括边界
		mArrOutputVertices = new Array<Vertex>(7);
		mArrRotatedVertices = new Array<Vertex>(4);
		mArrIntersections = new Array<Vertex>(2);
		mArrTempVertices = new Array<Vertex>(7 + 4);

		//实例化缓存的顶点
		for (int i = 0; i < 7 + 4; ++i) {
			mArrTempVertices.add(new Vertex());
		}

		mArrShadowSelfVertices = new Array<ShadowVertex>(
				(mMaxCurlSplits + 2) * 2);
		mArrShadowDropVertices = new Array<ShadowVertex>(
				(mMaxCurlSplits + 2) * 2);
		mArrShadowTempVertices = new Array<ShadowVertex>(
				(mMaxCurlSplits + 2) * 2);
		for (int i = 0; i < (mMaxCurlSplits + 2) * 2; ++i) {
			mArrShadowTempVertices.add(new ShadowVertex());
		}

		//填充四边形的四个顶点 0左上  1 左下  2右上 3右下
		for (int i = 0; i < 4; ++i) {
			mRectangle[i] = new Vertex();
		}
		//设置每个顶点阴影方向 ，基于这些值来计算阴影
		mRectangle[0].mPenumbraX = mRectangle[1].mPenumbraX = mRectangle[1].mPenumbraY = mRectangle[3].mPenumbraY = -1;
		mRectangle[0].mPenumbraY = mRectangle[2].mPenumbraX = mRectangle[2].mPenumbraY = mRectangle[3].mPenumbraX = 1;

		// 边界矩形包含四个顶点 , 分割两个角的线占用了两个顶点  弯曲效果包含 分割线数目乘以2数量的顶点
		int maxVerticesCount = 4 + 2 + (2 * mMaxCurlSplits);

		//申请本地内存
		mBufVertices = ByteBuffer.allocateDirect(maxVerticesCount * 3 * 4)
				.order(ByteOrder.nativeOrder()).asFloatBuffer();
		mBufVertices.position(0);

		mBufTexCoords = ByteBuffer.allocateDirect(maxVerticesCount * 2 * 4)
				.order(ByteOrder.nativeOrder()).asFloatBuffer();
		mBufTexCoords.position(0);

		mBufNormals = ByteBuffer.allocateDirect(maxVerticesCount * 3 * 4)
				.order(ByteOrder.nativeOrder()).asFloatBuffer();
		mBufNormals.position(0);

		int maxShadowVerticesCount = (mMaxCurlSplits + 2) * 2 * 2;
		mBufShadowVertices = ByteBuffer
				.allocateDirect(maxShadowVerticesCount * 3 * 4)
				.order(ByteOrder.nativeOrder()).asFloatBuffer();
		mBufShadowVertices.position(0);

		mBufShadowPenumbra = ByteBuffer
				.allocateDirect(maxShadowVerticesCount * 2 * 4)
				.order(ByteOrder.nativeOrder()).asFloatBuffer();
		mBufShadowPenumbra.position(0);
	}

	/**
	 * 添加顶点数据到本地缓存中
	 */
	private void addVertex(Vertex vertex) {
		mBufVertices.put((float) vertex.mPosX);
		mBufVertices.put((float) vertex.mPosY);
		mBufVertices.put((float) vertex.mPosZ);
		mBufTexCoords.put((float) vertex.mTexX);
		mBufTexCoords.put((float) vertex.mTexY);
		mBufNormals.put((float) vertex.mNormalX);
		mBufNormals.put((float) vertex.mNormalY);
		mBufNormals.put((float) vertex.mNormalZ);
	}

	/**
	 * Sets curl for this mesh.
	 * 
	 * @param curlPos
	 *            手指拖动时按下的位置
	 * @param curlDir
	 *           卷曲的方向，传入这个参数前  这个参数需要做归一化处理
	 * @param radius
	 *            卷曲半径
	 */
	public void curl(PointF curlPos, PointF curlDir, double radius) {

		//将缓存中的位置置0，从第一个位置开始读取字节数据
		mBufVertices.position(0);
		mBufTexCoords.position(0);
		mBufNormals.position(0);
		// 计算 绕Z轴 旋转的角度  由于是单位向量，所以curlDir.x / 1  就是余弦值
		double curlAngle = Math.acos(curlDir.x);
		//curlDir.y > 0 从下往上卷曲
		curlAngle = curlDir.y > 0 ? -curlAngle : curlAngle;


		// 初始化旋转矩形 向触摸点平移然后旋转，卷曲的方向指向 右边（1,0）的位置. 同时顶点按照x轴坐标升序排列
		// 两个顶点有很小的几率会有同样的x值此时就按照y轴的大小来排列。
		mArrTempVertices.addAll(mArrRotatedVertices);//缓存顶点的容器 添加了矩形的四个顶点
		mArrRotatedVertices.clear();
		for (int i = 0; i < 4; ++i) {
			Vertex v = mArrTempVertices.remove(0);
			v.set(mRectangle[i]);//分别设置矩形的四个顶点
			v.translate(-curlPos.x, -curlPos.y);//从矩形顶点平移到对应的触摸点
			//绕Z轴旋转-curlAngle之后的点
			v.rotateZ(-curlAngle);
			int j = 0;
			for (; j < mArrRotatedVertices.size(); ++j) {
				//旋转之前的顶点
				Vertex v2 = mArrRotatedVertices.get(j);
				if (v.mPosX > v2.mPosX) {
					break;
				}
				if (v.mPosX == v2.mPosX && v.mPosY > v2.mPosY) {
					break;
				}
			}
			mArrRotatedVertices.add(j, v);
		}

		// 旋转的矩形的线条和定点的 index顺序. 我们需要找到旋转矩形的边界对应的线条. 在定点都按照x轴的顺序排序之后
		// 偶们没必要担心在 0和1位置的顶点所确定的那条直线
		// 但是由于精度的问题，很有可能在3位置的顶点不是在0顶点 的对面
		// 所以我们需要计算 0 点到 2和3点 的距离。然后根据需要 变更 line的顺序。
		// 顶点所组成的线段 被按照x轴的顺序给出。
		int lines[][] = { { 0, 1 }, { 0, 2 }, { 1, 3 }, { 2, 3 } };//相邻顶点之间的连线
		{
			// TODO: There really has to be more 'easier' way of doing this -
			// not including extensive use of sqrt.
			Vertex v0 = mArrRotatedVertices.get(0);
			Vertex v2 = mArrRotatedVertices.get(2);
			Vertex v3 = mArrRotatedVertices.get(3);

			//求出v0到v2的距离
			double dist2 = Math.sqrt((v0.mPosX - v2.mPosX)
					* (v0.mPosX - v2.mPosX) + (v0.mPosY - v2.mPosY)
					* (v0.mPosY - v2.mPosY));
			//求出v0到v3的距离
 			double dist3 = Math.sqrt((v0.mPosX - v3.mPosX)
					* (v0.mPosX - v3.mPosX) + (v0.mPosY - v3.mPosY)
					* (v0.mPosY - v3.mPosY));

			//根据距离来判断2 ，3点的位置
 			if (dist2 > dist3) {
				lines[1][1] = 3;//  0 1 2 3
				lines[2][1] = 2;
			}//  0 1 3 2
		}

		mCountVertices = 0;

		mArrShadowTempVertices.addAll(mArrShadowDropVertices);
		mArrShadowTempVertices.addAll(mArrShadowSelfVertices);
		mArrShadowDropVertices.clear();
		mArrShadowSelfVertices.clear();

		// 弯曲的曲线的长度
		double curlLength = Math.PI * radius;
		// 计算分割线
		// TODO: Revisit this code one day. There is room for optimization here.
		mArrScanLines.clear();
		if (mMaxCurlSplits > 0) {
			//先添加最左边的线 x=0
			mArrScanLines.add((double) 0);
		}
		for (int i = 1; i < mMaxCurlSplits; ++i) {
			mArrScanLines.add((-curlLength * i) / (mMaxCurlSplits - 1));
		}
		// 旋转的4个边界顶点 按x轴坐标点的顺序排列, 添加分割线切割出来的区域，跳出旋转过的顶点
		//添加最右边的顶点 对应的分割线x=mArrRotatedVertices.get(3).mPosX - 1
		mArrScanLines.add(mArrRotatedVertices.get(3).mPosX - 1);

		// 从最右边的顶点开始. Pretty much the same as first scan area
		// is starting from 'infinity'.
		double scanXmax = mArrRotatedVertices.get(0).mPosX + 1;

		for (int i = 0; i < mArrScanLines.size(); ++i) {
			//一旦计算出scanXmin和scanXmax 就可以确定出一个大概的区域
			double scanXmin = mArrScanLines.get(i);
			// 首先在可视区域内迭代 最初的矩形的顶点
			for (int j = 0; j < mArrRotatedVertices.size(); ++j) {
				Vertex v = mArrRotatedVertices.get(j);
				// 测试这个顶点是否在区域内
				// TODO: Frankly speaking, can't remember why equality check was
				// added to both ends. Guessing it was somehow related to case
				// where radius=0f, which, given current implementation, could
				// be handled much more effectively anyway.
				if (v.mPosX >= scanXmin && v.mPosX <= scanXmax) {
					// 从mArrTempVertices中取出一个顶点
					Vertex n = mArrTempVertices.remove(0);
					//并将符合条件的n设置到这个点
					n.set(v);
					// This is done solely for triangulation reasons. Given a
					// rotated rectangle it has max 2 vertices having
					// intersection.
					Array<Vertex> intersections = getIntersections(
							mArrRotatedVertices, lines, n.mPosX);
					// In a sense one could say we're adding vertices always in
					// two, positioned at the ends of intersecting line. And for
					// 三角化 to work properly they are added based on y
					// -coordinate. And this if-else is doing it for us.
					if (intersections.size() == 1
							&& intersections.get(0).mPosY > v.mPosY) {
						// In case intersecting vertex is higher add it first.
						mArrOutputVertices.addAll(intersections);
						mArrOutputVertices.add(n);
					} else if (intersections.size() <= 1) {
						// Otherwise add original vertex first.
						mArrOutputVertices.add(n);
						mArrOutputVertices.addAll(intersections);
					} else {
						// There should never be more than 1 intersecting
						// vertex. But if it happens as a fallback simply skip
						// everything.
						mArrTempVertices.add(n);
						mArrTempVertices.addAll(intersections);
					}
				}
			}

			// Search for scan line intersections.计算出交点
			Array<Vertex> intersections = getIntersections(mArrRotatedVertices,
					lines, scanXmin);

			// We expect to get 0 or 2 vertices. In rare cases there's only one
			// but in general given a scan line intersecting rectangle there
			// should be 2 intersecting vertices.
			if (intersections.size() == 2) {
				// There were two intersections, add them based on y
				// -coordinate, higher first, lower last.

				Vertex v1 = intersections.get(0);
				Vertex v2 = intersections.get(1);
				if (v1.mPosY < v2.mPosY) {
					mArrOutputVertices.add(v2);
					mArrOutputVertices.add(v1);
				} else {
					mArrOutputVertices.addAll(intersections);
				}
			} else if (intersections.size() != 0) {
				// This happens in a case in which there is a original vertex
				// exactly at scan line or something went very much wrong if
				// there are 3+ vertices. What ever the reason just return the
				// vertices to temp vertices for later use. In former case it
				// was handled already earlier once iterating through
				// mRotatedVertices, in latter case it's better to avoid doing
				// anything with them.
				mArrTempVertices.addAll(intersections);
			}

			// 通过迭代将找到的顶点添加到缓存中
			while (mArrOutputVertices.size() > 0) {
				Vertex v = mArrOutputVertices.remove(0);
				mArrTempVertices.add(v);

				// Local texture front-facing flag.
				// boolean textureFront;

				// Untouched vertices.
				if (i == 0) {
					v.mNormalX = 0;
					v.mNormalY = 0;
					v.mNormalZ = 1;
				}
				// 彻底旋转的顶点
				else if (i == mArrScanLines.size() - 1 || curlLength == 0) {
					v.mPosX = -(curlLength + v.mPosX);
					v.mPosZ = 2 * radius;
					v.mNormalX = 0;
					v.mNormalY = 0;
					v.mNormalZ = -1;
					v.mPenumbraX = -v.mPenumbraX;
				}
				// Vertex lies within 'curl'.
				else {
					// Even though it's not obvious from the if-else clause,
					// here v.mPosX is between [-curlLength, 0]. And we can do
					// 围绕一个半圆柱进行计算.
					double rotY = Math.PI * (v.mPosX / curlLength);
					v.mPosX = radius * Math.sin(rotY);
					v.mPosZ = radius - (radius * Math.cos(rotY));
					v.mNormalX = Math.sin(rotY);
					v.mNormalY = 0;
					v.mNormalZ = Math.cos(rotY);
					v.mPenumbraX *= Math.cos(rotY);
				}

				// Move vertex back to 'world' coordinates.
				v.rotateZ(curlAngle);
				v.translate(curlPos.x, curlPos.y);
				addVertex(v);
				++mCountVertices;

				// Drop shadow is cast 'behind' the curl.
				if (v.mPosZ > 0 && v.mPosZ <= radius) {
					ShadowVertex sv = mArrShadowTempVertices.remove(0);
					sv.mPosX = v.mPosX;
					sv.mPosY = v.mPosY;
					sv.mPosZ = v.mPosZ;
					sv.mPenumbraX = (v.mPosZ * 0.8) * -curlDir.x;
					sv.mPenumbraY = (v.mPosZ * 0.8) * -curlDir.y;
					// sv.mPenumbraX += (v.mPosZ * 0.8) * v.mPenumbraX;
					// sv.mPenumbraY += (v.mPosZ * 0.8) * v.mPenumbraY;
					int idx = (mArrShadowDropVertices.size() + 1) / 2;
					mArrShadowDropVertices.add(idx, sv);
				}
				// 自身的阴影部分投射在mesh上
				if (v.mPosZ > radius) {
					ShadowVertex sv = mArrShadowTempVertices.remove(0);
					sv.mPosX = v.mPosX;
					sv.mPosY = v.mPosY;
					sv.mPosZ = v.mPosZ;
					sv.mPenumbraX = ((v.mPosZ - radius) * 0.2) * v.mPenumbraX;
					sv.mPenumbraY = ((v.mPosZ - radius) * 0.2) * v.mPenumbraY;
					int idx = (mArrShadowSelfVertices.size() + 1) / 2;
					mArrShadowSelfVertices.add(idx, sv);
				}
			}

			//将scanXmax的值转换为scanXmin 用于下一次迭代
			scanXmax = scanXmin;
		}

		mBufVertices.position(0);//顶点
		mBufTexCoords.position(0);//纹理
		mBufNormals.position(0);//方向

		// 添加阴影顶点
		mBufShadowVertices.position(0);
		mBufShadowPenumbra.position(0);
		mCountShadowDrop = mCountShadowSelf = 0;

		for (int i = 0; i < mArrShadowDropVertices.size(); ++i) {
			ShadowVertex sv = mArrShadowDropVertices.get(i);
			mBufShadowVertices.put((float) sv.mPosX);
			mBufShadowVertices.put((float) sv.mPosY);
			mBufShadowVertices.put((float) sv.mPosZ);
			mBufShadowPenumbra.put(0);
			mBufShadowPenumbra.put(0);
			mBufShadowVertices.put((float) sv.mPosX);
			mBufShadowVertices.put((float) sv.mPosY);
			mBufShadowVertices.put((float) sv.mPosZ);
			mBufShadowPenumbra.put((float) sv.mPenumbraX);
			mBufShadowPenumbra.put((float) sv.mPenumbraY);
			mCountShadowDrop += 2;
		}
		for (int i = 0; i < mArrShadowSelfVertices.size(); ++i) {
			ShadowVertex sv = mArrShadowSelfVertices.get(i);
			mBufShadowVertices.put((float) sv.mPosX);
			mBufShadowVertices.put((float) sv.mPosY);
			mBufShadowVertices.put((float) sv.mPosZ);
			mBufShadowPenumbra.put(0);
			mBufShadowPenumbra.put(0);
			mBufShadowVertices.put((float) sv.mPosX);
			mBufShadowVertices.put((float) sv.mPosY);
			mBufShadowVertices.put((float) sv.mPosZ);
			mBufShadowPenumbra.put((float) sv.mPenumbraX);
			mBufShadowPenumbra.put((float) sv.mPenumbraY);
			mCountShadowSelf += 2;
		}
		mBufShadowVertices.position(0);
		mBufShadowPenumbra.position(0);
	}

	/**
	 * 获取投影顶点的数量.
	 */
	public int getDropShadowCount() {
		return mCountShadowDrop;
	}

	/**
	 * 获取page是否翻动
	 */
	public boolean getFlipTexture() {
		return mFlipTexture;
	}

	/**
	 * 计算给出的线段与 x=scanX线段的交点
	 * 按照顺序排列顶点得出不同的线段
	 */
	private Array<Vertex> getIntersections(Array<Vertex> vertices,
			int[][] lineIndices, double scanX) {
		mArrIntersections.clear();
		//四边形的每一条边都可以用一对顶点来表示
		for (int j = 0; j < lineIndices.length; j++) {
			Vertex v1 = vertices.get(lineIndices[j][0]);
			Vertex v2 = vertices.get(lineIndices[j][1]);
			if (v1.mPosX > scanX && v2.mPosX < scanX) {
				//有一个交点，计算系数 来判断直线上x=scanX的点距离v2多远
				double c = (scanX - v2.mPosX) / (v1.mPosX - v2.mPosX);
				Vertex n = mArrTempVertices.remove(0);
				n.set(v2);
				//交点的x坐标
				n.mPosX = scanX;
				//设置交点的y坐标
				n.mPosY += (v1.mPosY - v2.mPosY) * c;
				n.mTexX += (v1.mTexX - v2.mTexX) * c;
				n.mTexY += (v1.mTexY - v2.mTexY) * c;
				n.mPenumbraX += (v1.mPenumbraX - v2.mPenumbraX) * c;
				n.mPenumbraY += (v1.mPenumbraY - v2.mPenumbraY) * c;
				mArrIntersections.add(n);
			}
		}
		return mArrIntersections;
	}

	/**
	 * 获取 方向 对应的缓存
	 */
	public FloatBuffer getNormals() {
		return mBufNormals;
	}

	/**
	 * 获取这个mesh对应的page对象
	 */
	public CurlPage getPage() {
		return mPage;
	}

	/**
	 * 获取阴影顶点数量
	 */
	public int getSelfShadowCount() {
		return mCountShadowSelf;
	}

	/**
	 * penumbra中文为“半影”，是仅能接受到有限大光源部分光照的区域。有限大光源产生半影，使得阴影的边沿柔和化，也称作Soft Shadow
	 * 这里是获取半影的本地缓存
	 */
	public FloatBuffer getShadowPenumbra() {
		return mBufShadowPenumbra;
	}

	/**
	 * 获取阴影效果的顶点缓存
	 */
	public FloatBuffer getShadowVertices() {
		return mBufShadowVertices;
	}

	/**
	 * 获取纹理映射对应位置的缓存.
	 */
	public FloatBuffer getTexCoords() {
		return mBufTexCoords;
	}

	/**
	 * 获取纹理id，必须在GL线程中执行
	 */
	public int[] getTextures() {

		//第一次如果没有纹理id的话就直接创建
		if (mTextureIds == null) {
			// 生成纹理
			mTextureIds = new int[2];
			GLES20.glGenTextures(2, mTextureIds, 0);
			for (int textureId : mTextureIds) {
				// 设置纹理属性
				GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
				GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
						GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
				GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
						GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
				GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
						GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
				GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
						GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
			}
		}

		if (mPage.getBitmapsChanged()) {
			GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureIds[0]);
			GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0,
					mPage.getBitmap(CurlPage.SIDE_FRONT), 0);
			GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureIds[1]);
			GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0,
					mPage.getBitmap(CurlPage.SIDE_BACK), 0);
			mPage.recycle();
		}

		return mTextureIds;
	}

	/**
	 * 获取顶点数量
	 */
	public int getVertexCount() {
		return mCountVertices;
	}

	/**
	 * 获取顶点的本地缓存
	 */
	public FloatBuffer getVertices() {
		return mBufVertices;
	}

	/**
	 * 将mesh重置到初始状态. 也就是说在调用这个方法后
	 * 可以在mesh对应的四边形上画一个完整的 没有扭曲的纹理
	 */
	public void reset() {
		mBufVertices.position(0);
		mBufTexCoords.position(0);
		mBufNormals.position(0);
		for (int i = 0; i < 4; ++i) {
			Vertex tmp = mArrTempVertices.get(0);
			tmp.set(mRectangle[i]);
			addVertex(tmp);
		}
		mCountVertices = 4;
		mBufVertices.position(0);
		mBufTexCoords.position(0);
		mBufNormals.position(0);
		mCountShadowDrop = mCountShadowSelf = 0;
	}

	/**
	 * 重置并创建新的纹理id. After calling
	 * this method you most likely want to set bitmap too as it's lost.
	 * 一旦GL context被重新创建了，这个方法就应该被调用一次。 这个方法不会释放上一章纹理的id，它
	 * 只能确保下一次渲染时会请求新的纹理id
	 */
	public void resetTextures() {
		mTextureIds = null;
	}

	/**
	 * 为true时 page两边都会映射纹理
	 */
	public void setFlipTexture(boolean flipTexture) {
		mFlipTexture = flipTexture;
		if (flipTexture) {
			setTexCoords(1f, 0f, 0f, 1f);
		} else {
			setTexCoords(0f, 0f, 1f, 1f);
		}
	}

	/**
	 * 更新平面四个点的x,y坐标
	 */
	public void setRect(RectF r) {
		mRectangle[0].mPosX = r.left;
		mRectangle[0].mPosY = r.top;
		mRectangle[1].mPosX = r.left;
		mRectangle[1].mPosY = r.bottom;
		mRectangle[2].mPosX = r.right;
		mRectangle[2].mPosY = r.top;
		mRectangle[3].mPosX = r.right;
		mRectangle[3].mPosY = r.bottom;
	}

	/**
	 * 设置四边形纹理映射的顺序
	 */
	private void setTexCoords(float left, float top, float right, float bottom) {
		//左上
	    mRectangle[0].mTexX = left;
		mRectangle[0].mTexY = top;

		//左下
		mRectangle[1].mTexX = left;
		mRectangle[1].mTexY = bottom;

		//右上
		mRectangle[2].mTexX = right;
		mRectangle[2].mTexY = top;

		//右下
		mRectangle[3].mTexX = right;
		mRectangle[3].mTexY = bottom;
	}

	/**
	 * 固定大小 的数组 实现
	 */
	private class Array<T> {
		private Object[] mArray;
		private int mCapacity;
		private int mSize;

		public Array(int capacity) {
			mCapacity = capacity;
			mArray = new Object[capacity];
		}

		public void add(int index, T item) {
			if (index < 0 || index > mSize || mSize >= mCapacity) {
				throw new IndexOutOfBoundsException();
			}
			for (int i = mSize; i > index; --i) {
				mArray[i] = mArray[i - 1];
			}
			mArray[index] = item;
			++mSize;
		}

		public void add(T item) {
			if (mSize >= mCapacity) {
				throw new IndexOutOfBoundsException();
			}
			mArray[mSize++] = item;
		}

		public void addAll(Array<T> array) {
			if (mSize + array.size() > mCapacity) {
				throw new IndexOutOfBoundsException();
			}
			for (int i = 0; i < array.size(); ++i) {
				mArray[mSize++] = array.get(i);
			}
		}

		public void clear() {
			mSize = 0;
		}

		@SuppressWarnings("unchecked")
		public T get(int index) {
			if (index < 0 || index >= mSize) {
				throw new IndexOutOfBoundsException();
			}
			return (T) mArray[index];
		}

		@SuppressWarnings("unchecked")
		public T remove(int index) {
			if (index < 0 || index >= mSize) {
				throw new IndexOutOfBoundsException();
			}
			T item = (T) mArray[index];
			for (int i = index; i < mSize - 1; ++i) {
				mArray[i] = mArray[i + 1];
			}
			--mSize;
			return item;
		}

		public int size() {
			return mSize;
		}

	}

	/**
	 * 缓存阴影信息
	 */
	private class ShadowVertex {
		public double mPenumbraX;
		public double mPenumbraY;
		public double mPosX;
		public double mPosY;
		public double mPosZ;
	}

	/**
	 * 封装顶点信息
	 */
	private class Vertex {
	    //方向  方向的描述是通过向量点来描述的（0，0，0）到（x,y,z）
		public double mNormalX;
		public double mNormalY;
		public double mNormalZ;
		
		public double mPenumbraX;
		public double mPenumbraY;
		//位置
		public double mPosX;
		public double mPosY;
		public double mPosZ;
		//纹理
		public double mTexX;
		public double mTexY;

		public Vertex() {
			mNormalX = mNormalY = 0;
			mNormalZ = 1;
			mPosX = mPosY = mPosZ = mTexX = mTexY = 0;
		}


		//绕Z轴旋转
		public void rotateZ(double theta) {
			double cos = Math.cos(theta);   //x
			double sin = Math.sin(theta);    // y
			//位置
			double x = mPosX * cos + mPosY * sin;
			double y = mPosX * -sin + mPosY * cos;
			mPosX = x;
			mPosY = y;
			//方向
			double nx = mNormalX * cos + mNormalY * sin;
			double ny = mNormalX * -sin + mNormalY * cos;
			mNormalX = nx;
			mNormalY = ny;
			//半阴影
			double px = mPenumbraX * cos + mPenumbraY * sin;
			double py = mPenumbraX * -sin + mPenumbraY * cos;
			mPenumbraX = px;
			mPenumbraY = py;
		}

		/**
		 * 
		 * 方法描述：设置顶点属性
		 * @author 尤洋
		 * @Title: set
		 * @param vertex
		 * @return void
		 * @date 2015-4-28 下午7:15:58
		 */
		public void set(Vertex vertex) {
			mPosX = vertex.mPosX;
			mPosY = vertex.mPosY;
			mPosZ = vertex.mPosZ;
			mTexX = vertex.mTexX;
			mTexY = vertex.mTexY;
			mNormalX = vertex.mNormalX;
			mNormalY = vertex.mNormalY;
			mNormalZ = vertex.mNormalZ;
			mPenumbraX = vertex.mPenumbraX;
			mPenumbraY = vertex.mPenumbraY;
		}

		//顶点平移
		public void translate(double dx, double dy) {
			mPosX += dx;
			mPosY += dy;
		}
	}
}
