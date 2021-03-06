package org.nustaq.kontraktor.remoting.tcp;

import org.nustaq.kontraktor.*;
import org.nustaq.kontraktor.impl.BackOffStrategy;
import org.nustaq.kontraktor.impl.RemoteScheduler;
import org.nustaq.kontraktor.remoting.ObjectSocket;
import org.nustaq.kontraktor.remoting.RemoteRefRegistry;
import org.nustaq.kontraktor.util.Log;

import java.io.*;
import java.net.SocketException;

/**
 * Created by ruedi on 08.08.14.
 *
 * Client side for an tcp actor server.
 * actor refs/callbacks/futures handed out to the actors' server facade are automatically transformed
 * and rerouted, so remoting is mostly transparent.
 */
public class TCPActorClient<T extends Actor> extends RemoteRefRegistry {

    public static <AC extends Actor> Future<AC> Connect( Class<AC> clz, String host, int port ) throws IOException {
        Promise<AC> res = new Promise<>();
        TCPActorClient<AC> client = new TCPActorClient<>( clz, host, port);
        new Thread(() -> {
            try {
                client.connect();
                res.receive(client.getFacadeProxy(), null);
            } catch (IOException e) {
                Log.Warn(TCPActorClient.class,e,"");
                res.receive(null, e);
            }
        }, "connect "+client.getDescriptionString()).start();
        return res;
    }

    Class<? extends Actor> actorClazz;
    T facadeProxy;
    BackOffStrategy backOffStrategy = new BackOffStrategy();

    String host;
    int port;
    ActorClient client;
    volatile boolean connected = false;

    public TCPActorClient(Class<? extends Actor> clz, String host, int port) throws IOException {
        this.host = host;
        this.port = port;
        actorClazz = clz;
        facadeProxy = Actors.AsActor( actorClazz, new RemoteScheduler() );
        facadeProxy.__remoteId = 1;
        registerRemoteRefDirect(facadeProxy);
    }

    public T getFacadeProxy() {
        return facadeProxy;
    }

    public void connect() throws IOException {
        try {
            client = new ActorClient();
            connected = true;
            facadeProxy.__addRemoteConnection(client);
        } catch (IOException ioe) {
            throw ioe;
        } catch (Exception ex) {
            Log.Info(this,"connection to " + getDescriptionString() + " failed");
        }
    }

    private String getDescriptionString() {
        return actorClazz.getSimpleName() + "@" + host + ":" + port;
    }

    public boolean isConnected() {
        return connected;
    }

    /**
     *
     */
    public class ActorClient implements RemoteConnection {

        ObjectSocket chan;

        public ActorClient() throws IOException {
            chan = new TCPSocket(host,port,conf);
            new Thread(
                () -> {
                    currentObjectSocket.set(chan);
                    try {
                        sendLoop(chan);
                    } catch (IOException e) {
                        if (e instanceof SocketException)
                            Log.Lg.infoLong(this,e,"");
                        else
                            Log.Warn(this,e,"");
                    }
                },
                "sender"
            ).start();
            new Thread(
                () -> {
                    currentObjectSocket.set(chan);
                    receiveLoop(chan);
                },
                "receiver"
            ).start();
        }

        public void close() {
            try {
                chan.close();
            } catch (IOException e) {
                Log.Warn(this,e,"");
            }
        }
    }

    @Override
    protected void remoteRefStopped(Actor actor) {
        super.remoteRefStopped(actor);
        if (actor.getActorRef() == facadeProxy.getActorRef() ) {
            // connection closed => close connection and stop all remoteRefs
            setTerminated(true);
            stopRemoteRefs();
            client.close();
        }
    }

}
