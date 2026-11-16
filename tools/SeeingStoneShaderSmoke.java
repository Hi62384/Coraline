import java.nio.*;
import java.nio.file.*;
import java.util.zip.ZipFile;
import org.joml.*;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryStack;
import static org.lwjgl.opengl.GL33C.*;

/** Actual hidden OpenGL shader + pixel readback, not Minecraft scene acceptance. */
public final class SeeingStoneShaderSmoke {
    private static int checks;
    private static int shader(int type,String text){
        int result=glCreateShader(type);glShaderSource(result,text);glCompileShader(result);
        if(glGetShaderi(result,GL_COMPILE_STATUS)==0)throw new AssertionError(glGetShaderInfoLog(result));return result;
    }
    private static void matrix(int program,String name,Matrix4f value){
        int at=glGetUniformLocation(program,name);if(at<0)throw new AssertionError("Missing uniform "+name);
        try(var stack=MemoryStack.stackPush()){glUniformMatrix4fv(at,false,value.get(stack.mallocFloat(16)));}
    }
    // CPU independent ray/section classification; discard the anti-aliased edge.
    private static int expected(Matrix4f inverse,float u,float v){
        var a=inverse.transform(new Vector4f(u*2-1,v*2-1,-1,1));a.div(a.w);
        var b=inverse.transform(new Vector4f(u*2-1,v*2-1,1,1));b.div(b.w);b.sub(a);
        double margin=Double.POSITIVE_INFINITY;
        for(int i=0;i<4;i++){
            double z=switch(i){case 0->.075;case 1->.035;case 2->-.035;default->-.075;};
            double t=(z-a.z)/b.z;if(t<0)return 0;
            double x=a.x+t*b.x,y=a.y+t*b.y;
            double boundary=.145*(1+.055*java.lang.Math.sin(java.lang.Math.atan2(y,x)+.4))+(i==0||i==3?.025:0);
            margin=java.lang.Math.min(margin,boundary-java.lang.Math.hypot(x,y));
        }
        // fwidth expands to more local-space units at wide FOV. Keep full-color
        // equality assertions several pixels away from that intentional blend.
        return java.lang.Math.abs(margin)<.015?-1:margin>0?1:0;
    }
    public static void main(String[] args)throws Exception{
        if(!GLFW.glfwInit())throw new AssertionError("GLFW unavailable");long window=0;
        try{
            GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE,GLFW.GLFW_FALSE);
            GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR,3);GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR,3);
            GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE,GLFW.GLFW_OPENGL_CORE_PROFILE);
            window=GLFW.glfwCreateWindow(64,64,"Coraline isolated shader QA",0,0);
            if(window==0)throw new AssertionError("Hidden OpenGL context unavailable");GLFW.glfwMakeContextCurrent(window);GL.createCapabilities();
            String vertex;try(var vanilla=new ZipFile(args[1])){vertex=new String(vanilla.getInputStream(vanilla.getEntry("assets/minecraft/shaders/program/blit.vsh")).readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);}
            int vs=shader(GL_VERTEX_SHADER,vertex),fs=shader(GL_FRAGMENT_SHADER,Files.readString(Path.of(args[0]))),program=glCreateProgram();glAttachShader(program,vs);glAttachShader(program,fs);glLinkProgram(program);
            if(glGetProgrami(program,GL_LINK_STATUS)==0)throw new AssertionError(glGetProgramInfoLog(program));glUseProgram(program);
            int vao=glGenVertexArrays();glBindVertexArray(vao);int vbo=glGenBuffers();glBindBuffer(GL_ARRAY_BUFFER,vbo);
            int position=glGetAttribLocation(program,"Position");glEnableVertexAttribArray(position);glVertexAttribPointer(position,2,GL_FLOAT,false,0,0L);
            int source=glGenTextures();glActiveTexture(GL_TEXTURE0);glBindTexture(GL_TEXTURE_2D,source);var color=ByteBuffer.allocateDirect(4).put(new byte[]{(byte)200,60,10,(byte)255});color.flip();
            glTexImage2D(GL_TEXTURE_2D,0,GL_RGBA8,1,1,0,GL_RGBA,GL_UNSIGNED_BYTE,color);glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MIN_FILTER,GL_NEAREST);glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MAG_FILTER,GL_NEAREST);glUniform1i(glGetUniformLocation(program,"DiffuseSampler"),0);
            int spectral=glGenTextures();glActiveTexture(GL_TEXTURE1);glBindTexture(GL_TEXTURE_2D,spectral);glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MIN_FILTER,GL_NEAREST);glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MAG_FILTER,GL_NEAREST);glUniform1i(glGetUniformLocation(program,"SpectralSampler"),1);glActiveTexture(GL_TEXTURE0);
            int framebuffer=glGenFramebuffers(),output=glGenTextures();glBindFramebuffer(GL_FRAMEBUFFER,framebuffer);
            glDisable(GL_BLEND);glDisable(GL_DEPTH_TEST);glDisable(GL_FRAMEBUFFER_SRGB);
            for(int scenario=0;scenario<8;scenario++){
                glActiveTexture(GL_TEXTURE1);glBindTexture(GL_TEXTURE_2D,spectral);var eyeColor=ByteBuffer.allocateDirect(4).put(scenario==7?new byte[]{0,0,0,0}:new byte[]{0,70,(byte)240,(byte)255});eyeColor.flip();glTexImage2D(GL_TEXTURE_2D,0,GL_RGBA8,1,1,0,GL_RGBA,GL_UNSIGNED_BYTE,eyeColor);glActiveTexture(GL_TEXTURE0);
                glUniform1f(glGetUniformLocation(program,"EyesEnabled"),scenario>=6?1:0);
                int width=scenario%2==0?512:640,height=scenario%2==0?384:360;
                glBindTexture(GL_TEXTURE_2D,output);glTexImage2D(GL_TEXTURE_2D,0,GL_RGBA8,width,height,0,GL_RGBA,GL_UNSIGNED_BYTE,(ByteBuffer)null);glFramebufferTexture2D(GL_FRAMEBUFFER,GL_COLOR_ATTACHMENT0,GL_TEXTURE_2D,output,0);
                if(glCheckFramebufferStatus(GL_FRAMEBUFFER)!=GL_FRAMEBUFFER_COMPLETE)throw new AssertionError("Incomplete readback target");
                glBindTexture(GL_TEXTURE_2D,source);glViewport(0,0,width,height);
                float fov=switch(scenario/2){case 0->35;case 1->70;default->110;};
                var projection=new Matrix4f().perspective((float)java.lang.Math.toRadians(fov),(float)width/height,.05F,1000);
                var pose=new Matrix4f().rotateZ(scenario%2==0?0:.16F).rotateX(scenario%2==0?0:.09F).translate(0,scenario%2==0?0:-.12F,-.42F).scale(.5F);
                var inverse=new Matrix4f(projection).mul(pose).invert();matrix(program,"ScreenToStone",inverse);
                matrix(program,"ProjMat",new Matrix4f().setOrtho(0,width,0,height,.1F,1000));glUniform2f(glGetUniformLocation(program,"OutSize"),width,height);
                glBufferData(GL_ARRAY_BUFFER,new float[]{0,0,width,0,width,height,0,0,width,height,0,height},GL_STATIC_DRAW);glDrawArrays(GL_TRIANGLES,0,6);
                var pixels=ByteBuffer.allocateDirect(width*height*4);glReadPixels(0,0,width,height,GL_RGBA,GL_UNSIGNED_BYTE,pixels);int gray=0,normal=0;
                for(int y=1;y<height;y+=3)for(int x=1;x<width;x+=3){
                    int expected=expected(inverse,(x+.5F)/width,(y+.5F)/height);if(expected<0)continue;int at=(y*width+x)*4,r=Byte.toUnsignedInt(pixels.get(at)),g=Byte.toUnsignedInt(pixels.get(at+1)),b=Byte.toUnsignedInt(pixels.get(at+2));checks++;
                    if(expected==1){gray++;int rr=scenario==6?0:86,gg=scenario==6?70:86,bb=scenario==6?240:86;if(java.lang.Math.abs(r-rr)>2||java.lang.Math.abs(g-gg)>2||java.lang.Math.abs(b-bb)>2)throw new AssertionError("Wrong inspected color: "+scenario+" @ "+x+","+y+" rgb="+r+","+g+","+b);}
                    else{normal++;if(r!=200||g!=60||b!=10)throw new AssertionError("Gray leaked outside hole: "+scenario+" @ "+x+","+y);}
                }
                if(gray<50||normal<1000)throw new AssertionError("Insufficient aperture/outside coverage");
                System.out.println("GPU_APERTURE scenario="+scenario+" fov="+fov+" size="+width+"x"+height+" gray="+gray+" normal="+normal);
            }
            if(glGetError()!=GL_NO_ERROR)throw new AssertionError("OpenGL error");
            System.out.println("SEEING_STONE_SHADER_PASSED checks="+checks+" renderer="+glGetString(GL_RENDERER)+"; NOT Minecraft scene acceptance");
            if(args.length>2)rescue(vertex,Path.of(args[2]),source,spectral,output,framebuffer,vbo);
            glDeleteTextures(source);glDeleteTextures(spectral);glDeleteTextures(output);glDeleteFramebuffers(framebuffer);glDeleteBuffers(vbo);glDeleteVertexArrays(vao);glDeleteProgram(program);glDeleteShader(vs);glDeleteShader(fs);
        }finally{if(window!=0)GLFW.glfwDestroyWindow(window);GLFW.glfwTerminate();}
    }
    private static void rescue(String vertex,Path fragment,int source,int depth,int output,int framebuffer,int vbo)throws Exception{
        int vs=shader(GL_VERTEX_SHADER,vertex),fs=shader(GL_FRAGMENT_SHADER,Files.readString(fragment)),program=glCreateProgram();glAttachShader(program,vs);glAttachShader(program,fs);glLinkProgram(program);if(glGetProgrami(program,GL_LINK_STATUS)==0)throw new AssertionError(glGetProgramInfoLog(program));glUseProgram(program);
        int width=128,height=80;glBindBuffer(GL_ARRAY_BUFFER,vbo);int position=glGetAttribLocation(program,"Position");glEnableVertexAttribArray(position);glVertexAttribPointer(position,2,GL_FLOAT,false,0,0L);glBufferData(GL_ARRAY_BUFFER,new float[]{0,0,width,0,width,height,0,0,width,height,0,height},GL_STATIC_DRAW);
        glBindFramebuffer(GL_FRAMEBUFFER,framebuffer);glActiveTexture(GL_TEXTURE0);glBindTexture(GL_TEXTURE_2D,output);glTexImage2D(GL_TEXTURE_2D,0,GL_RGBA8,width,height,0,GL_RGBA,GL_UNSIGNED_BYTE,(ByteBuffer)null);glFramebufferTexture2D(GL_FRAMEBUFFER,GL_COLOR_ATTACHMENT0,GL_TEXTURE_2D,output,0);glBindTexture(GL_TEXTURE_2D,source);glUniform1i(glGetUniformLocation(program,"DiffuseSampler"),0);
        glActiveTexture(GL_TEXTURE1);glBindTexture(GL_TEXTURE_2D,depth);glTexImage2D(GL_TEXTURE_2D,0,GL_R32F,1,1,0,GL_RED,GL_FLOAT,new float[]{.5F});glUniform1i(glGetUniformLocation(program,"DepthSampler"),1);glActiveTexture(GL_TEXTURE0);glViewport(0,0,width,height);
        matrix(program,"ProjMat",new Matrix4f().setOrtho(0,width,0,height,.1F,1000));matrix(program,"InverseViewProjection",new Matrix4f().scale(100,1,100));glUniform2f(glGetUniformLocation(program,"OutSize"),width,height);glUniform3f(glGetUniformLocation(program,"RoomHouse"),0,0,0);int count=0;
        for(int scenario=0;scenario<4;scenario++){
            float radius=scenario==2?20:60,offset=scenario==3?1000:0;glUniform1f(glGetUniformLocation(program,"Radius"),radius);glUniform1f(glGetUniformLocation(program,"Collapse"),scenario==0?0:1);glUniform1f(glGetUniformLocation(program,"GreenRoom"),0);glUniform1f(glGetUniformLocation(program,"BlurRadius"),scenario==0?8:0);glUniform3f(glGetUniformLocation(program,"CameraHouse"),offset,0,0);glDrawArrays(GL_TRIANGLES,0,6);
            var pixels=ByteBuffer.allocateDirect(width*height*4);glReadPixels(0,0,width,height,GL_RGBA,GL_UNSIGNED_BYTE,pixels);
            for(int y=2;y<height;y+=5)for(int x=2;x<width;x+=2){double world=(x+.5)/width*200-100+offset;boolean refuge=java.lang.Math.abs(world)<=34.5,white=scenario!=0&&!refuge&&java.lang.Math.abs(world)>radius+.6;boolean unchanged=scenario==0||refuge||java.lang.Math.abs(world)<radius-1.6;if(!white&&!unchanged)continue;int at=(y*width+x)*4;for(int channel=0;channel<3;channel++){int expected=white?255:new int[]{200,60,10}[channel],actual=Byte.toUnsignedInt(pixels.get(at+channel));if(java.lang.Math.abs(actual-expected)>1)throw new AssertionError("White collapse crossed refuge/failed edge, scenario="+scenario+" world="+world+" channel="+channel+" rgb="+actual);count++;}}
        }
        if(glGetError()!=GL_NO_ERROR)throw new AssertionError("Rescue shader OpenGL error");System.out.println("RESCUE_WORLD_SHADER_PASSED channels="+count+"; house preserved, edge-only white, normalized blur; NOT game camera acceptance");glDeleteProgram(program);glDeleteShader(vs);glDeleteShader(fs);
    }
}
