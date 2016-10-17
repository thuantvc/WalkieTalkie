package com.nevermore.walkietalkie.client;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;

import com.nevermore.walkietalkie.client.MainActivity;
import com.nevermore.walkietalkie.models.ChatChannel;
import com.nevermore.walkietalkie.models.ChatMessage;
import com.nevermore.walkietalkie.models.VoiceChannel;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

public class VoiceThread extends  Thread{
    private boolean running = true;
    private boolean recording = false;
    private boolean speaking = false;
    private AudioRecord input;
    private AudioTrack output;
    private MainActivity parent;
    private byte selected;
    private ArrayList<VoiceChannel> channels;
    private DatagramSocket ioSocket;
    private List<String> members = new ArrayList<>();

    public static final int PORT = 53730;
    public static final int SERVER_PORT = 53732;

    public static final int STATUS_AVAILABLE = 0;
    public static final int STATUS_AVAILABLE = 0;
    public static final int STATUS_AVAILABLE = 0;


    public VoiceThread(MainActivity ma) {
        parent = ma;
        init();
    }

    public short bytesToShort(byte[] bytes) {
        return ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getShort();
    }
    public byte[] shortToBytes(short value) {
        return ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(value).array();
    }

    public boolean init() {
        input = new AudioRecord(MediaRecorder.AudioSource.MIC, 44100, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, 44100);
        output = new AudioTrack(AudioManager.STREAM_VOICE_CALL, 44100, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, 44100, AudioTrack.MODE_STREAM);
        channels = new ArrayList<VoiceChannel>();
        try {
            ioSocket = new DatagramSocket(PORT);
        } catch (SocketException e) {
            e.printStackTrace();
            // TODO: Error handling
        }
        return true;
    }

    public void tcpMsg(ChatMessage msg) {
        VoiceChannel channel = getChannel();
        switch (msg.message.substring(0,5)) {
            case "STRSPK":
                channel.set((byte)1, msg.message.substring(6));
                startSpk();
                break;
            case "STPSPK":
                channel.set((byte)0, null);
                stopSpk();
                break;
        }
    }

    public VoiceChannel getChannel() {
        return channels.get(selected);
    }

    public void changeChannel(byte id) {
        selected = id;
        parent.ct.send(new ChatMessage(selected, "CHGCHN" + id, parent.username));
    }

    public boolean startRec() {
        recording = true;
        input.startRecording();
        parent.ct.send(new ChatMessage(selected, "STRSPK", parent.username));
        getChannel().setState(2);
        return true;
    }

    public boolean stopRec() {
        recording = false;
        input.stop();
        parent.ct.send(new ChatMessage(selected, "STPSPK", parent.username));
        getChannel().setState(0);
        return true;
    }

    public boolean startSpk() {
        speaking = true;
        output.play();
        return true;
    }

    public boolean stopSpk() {
        speaking = false;
        output.stop();
        output.flush();
        return true;
    }

    public void send(short val) {
        byte[] buf = new byte[3];
        byte[] shorter = new byte[2];
        shorter = shortToBytes(val);
        buf[0] = selected;
        buf[1] = shorter[0];
        buf[2] = shorter[1];
        DatagramPacket packet = new DatagramPacket(buf, buf.length, parent.serverAddress, SERVER_PORT);
        try {
            ioSocket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
            // TODO: Error handling
        }
    }

    public void recieve() {
        byte[] buf = new byte[3];
        byte[] shorter = new byte[2];
        short[] in = new short[1];
        DatagramPacket packet = new DatagramPacket(buf, buf.length);
        try {
            ioSocket.receive(packet);
        } catch (IOException e) {
            e.printStackTrace();
            // TODO: Error handling
        }
        if(buf[0] == selected) {
            shorter[0] = buf[1];
            shorter[1] = buf[2];
            in[0] = bytesToShort(shorter);
            output.write(in, 0, 1);
        }
    }

    public void kill() {
        running = false;
    }

    public void run() {
        short buff[] = new short[1];
        while(running) {
            if(recording) {
                input.read(buff, 0, 1);
                send(buff[0]);
            } else if(speaking) {
                recieve();
            }
        }
    }

}