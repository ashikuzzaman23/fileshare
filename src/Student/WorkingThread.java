package Student;

import Tools.NetworkUtil;

import javax.swing.JOptionPane;
import java.io.*;
import java.util.ArrayList;
import java.util.Scanner;

/**
 * Created by Toufik on 9/27/2017.
 */
public class WorkingThread implements Runnable {
    NetworkUtil netUtil;
    StudentLogInController controller;
    Thread thread;
    public WorkingThread(NetworkUtil nu,StudentLogInController sc)throws IOException
    {
        this.netUtil = nu;
        this.controller = sc;
        thread = new Thread(this);
        thread.start();
    }
    @Override
    public void run() {
        outer:while(true)
        {
            String str = (String) netUtil.read();
            if(str==null)
            {

            }
            else if(str.equals("send"))
            {
                String msz = controller.Receiver.getText();
                netUtil.write(msz);
                msz =(String) netUtil.read();
                if(msz.equals("yes"))
                {
                    File file = new File(controller.filePath.getText());
                    long size = file.length();
                    netUtil.write(size);
                    msz=(String)netUtil.read();
                    if(msz.equals("yes"))
                    {
                        controller.log.appendText("File sending started\n");
                        String name = file.getName();
                        netUtil.write(name);
                        long chunk = (long)netUtil.read();
                        controller.log.appendText("Chunk size: "+(chunk/1000)+"KB\n");
                        InputStream in = null;
                        try {
                            in = new FileInputStream(file);
                            while(true)
                            {
                                byte[] bytes;
                                if(!(in.available()<chunk))
                                {
                                    bytes = new byte[(int)chunk];
                                }
                                else
                                {
                                    bytes = new byte[in.available()];
                                }

                                in.read(bytes);
                                netUtil.write(bytes);
                                netUtil.socket.setSoTimeout(30000);
                                String ack = (String)netUtil.tread();
                                if(ack.equals("nob"))
                                {
                                    controller.log.appendText("Server buffer is full, try later\n");
                                    break;
                                }
                                else
                                {
                                    if (in.available()<=0)
                                    {
                                        String rep = "ok";
                                        netUtil.write(rep);
                                        rep = (String) netUtil.read();
                                        controller.log.appendText(rep+"\n");
                                        break;
                                    }

                                }
                            }
                        } catch (Exception e) {
                            if(e instanceof InterruptedIOException)
                            {
                                String reply ="time out";
                                netUtil.write(reply);
                            }
                        }
                    }
                    else
                    {
                        controller.log.appendText("Server buffer is full, try later\n");
                    }
                }
                else
                {
                    controller.log.appendText("Receiver is Offline\n");
                }
            }
            else if(str.equals("receive"))
            {
                String fileID = (String) netUtil.read();
                String msz = "send";
                netUtil.write(msz);
                netUtil.write(fileID);
                String[] info = fileID.split(":");
                String title = "Do you want to download\n";
                title+=info[2]+"\n";
                title+="sent by: "+info[0]+"\n";
                int a=JOptionPane.showConfirmDialog(null,title);
                String result;
                if(a==JOptionPane.YES_OPTION)
                {
                    result="ok";
                }
                else
                {
                    result="no";
                }

                if (result.equals("ok"))
                {
                    String rep ="yes";
                    netUtil.write(rep);
                    ArrayList<byte[]> fileChunks =new ArrayList<>();
                    while(true)
                    {
                        Object object=netUtil.read();
                        if(object instanceof byte[])
                        {
                            fileChunks.add((byte[])object);
                        }
                        else
                        {
                            rep = (String) object;
                            if(rep.equals("complete"))
                            {
                                File folder = new File("Downloads");
                                if(!folder.exists())
                                {
                                    folder.mkdir();
                                }
                                File newFile =new File(folder.getAbsoluteFile()+File.separator+info[2]);
                                try {
                                    newFile.createNewFile();
                                    OutputStream out = new FileOutputStream(newFile);
                                    for(int j=0;j<fileChunks.size();j++)
                                    {
                                        out.write(fileChunks.get(j));
                                        out.flush();
                                    }
                                    out.close();
                                    controller.log.appendText("File Successfully Downloaded\n");
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                break;
                            }
                        }
                    }
                }
                else
                {
                    String rep ="no";
                    netUtil.write(rep);
                }
            }
            else if(str.equals("cDisconnect"))
            {
                netUtil.closeConnection();
                break outer;
            }
        }
    }
}
