import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import static org.lwjgl.opengl.GL33C.*;

/** Actual Minecraft render-target depth copy, in a tiny hidden native context. */
public final class SeeingStoneDepthSmoke {
    public static void main(String[] args){
        if(!GLFW.glfwInit())throw new AssertionError("GLFW unavailable");long window=0;
        TextureTarget world=null,eyes=null;
        try{
            GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE,GLFW.GLFW_FALSE);GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR,3);GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR,3);GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE,GLFW.GLFW_OPENGL_CORE_PROFILE);
            window=GLFW.glfwCreateWindow(64,64,"Coraline isolated depth QA",0,0);if(window==0)throw new AssertionError("No native context");GLFW.glfwMakeContextCurrent(window);GL.createCapabilities();
            RenderSystem.initRenderThread();RenderSystem.initGameThread(false);RenderSystem.initRenderer(0,false);
            world=new TextureTarget(64,64,true,false);eyes=new TextureTarget(64,64,true,false);
            for(int frame=0;frame<3;frame++){
                int width=frame==0?64:96,height=frame==0?64:80;
                if(frame==1){world.resize(width,height,false);eyes.resize(width,height,false);}
                world.bindWrite(true);glDepthMask(true);double expected=frame==0?.35:frame==1?.20:.65;glClearDepth(expected);glClear(GL_DEPTH_BUFFER_BIT);glClearDepth(1);
                eyes.setClearColor(0,0,0,0);eyes.clear(false);eyes.copyDepthFrom(world);eyes.bindWrite(true);
                float[] depth={0};glReadPixels(width/2,height/2,1,1,GL_DEPTH_COMPONENT,GL_FLOAT,depth);
                if(java.lang.Math.abs(depth[0]-expected)>.00001)throw new AssertionError("World occlusion was lost on depth copy/resize: "+depth[0]+" expected="+expected);
                var color=java.nio.ByteBuffer.allocateDirect(4);glReadPixels(width/2,height/2,1,1,GL_RGBA,GL_UNSIGNED_BYTE,color);if(color.get(3)!=0)throw new AssertionError("Unpainted eye layer is not transparent");
                System.out.println("MINECRAFT_DEPTH_COPY frame="+frame+" size="+width+"x"+height+" depth="+depth[0]+" clearAlpha=0");
                world.setClearColor(.12F,.34F,.56F,1);eyes.setClearColor(.85F,.17F,.2F,1);eyes.clear(false);
                com.coraline.client.SeeingStoneFramebuffer.copyColor(eyes,world);world.bindWrite(true);
                glReadPixels(width/2,height/2,1,1,GL_DEPTH_COMPONENT,GL_FLOAT,depth);
                if(java.lang.Math.abs(depth[0]-expected)>.00001)throw new AssertionError("Lens color copy cleared world depth");
                glReadPixels(width/2,height/2,1,1,GL_RGBA,GL_UNSIGNED_BYTE,color);
                if((color.get(0)&255)<210)throw new AssertionError("Lens color was not copied");
                world.clear(false);world.bindWrite(true);glReadPixels(width/2,height/2,1,1,GL_RGBA,GL_UNSIGNED_BYTE,color);
                if(java.lang.Math.abs((color.get(0)&255)-31)>1||java.lang.Math.abs((color.get(1)&255)-87)>1||java.lang.Math.abs((color.get(2)&255)-143)>1)throw new AssertionError("Lens copy corrupted cached sky clear color");
                System.out.println("MINECRAFT_LENS_COLOR_COPY preserves cached sky clear color and world depth");
            }
            if(glGetError()!=GL_NO_ERROR)throw new AssertionError("OpenGL error");
            System.out.println("SEEING_STONE_DEPTH_PASSED; real Minecraft TextureTarget; NOT full game/camera acceptance");
        }finally{if(eyes!=null)eyes.destroyBuffers();if(world!=null)world.destroyBuffers();if(window!=0)GLFW.glfwDestroyWindow(window);GLFW.glfwTerminate();}
    }
}
