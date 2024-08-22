package org.vinni.servidor.gui;


import javax.swing.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

/**
 * Author: Vinni
 */
public class PrincipalSrv extends javax.swing.JFrame {
    private final int PORT = 12345;
    private ServerSocket serverSocket;
    private javax.swing.JButton bIniciar;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JTextArea mensajesTxt;
    private javax.swing.JScrollPane jScrollPane1;
    private Map<String, ClientHandler> clientSockets = new HashMap<>();
    private boolean running = true;

    /**
     * Creates new form Principal1
     */
    public PrincipalSrv() {
        initComponents();
    }
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">
    private void initComponents() {
        this.setTitle("Servidor ...");

        bIniciar = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        mensajesTxt = new JTextArea();
        jScrollPane1 = new javax.swing.JScrollPane();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        getContentPane().setLayout(null);

        bIniciar.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        bIniciar.setText("INICIAR SERVIDOR");
        bIniciar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bIniciarActionPerformed(evt);
                bIniciar.setEnabled(false);
            }
        });
        getContentPane().add(bIniciar);
        bIniciar.setBounds(100, 90, 250, 40);

        jLabel1.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        jLabel1.setForeground(new java.awt.Color(204, 0, 0));
        jLabel1.setText("SERVIDOR TCP");
        getContentPane().add(jLabel1);
        jLabel1.setBounds(150, 10, 160, 17);

        mensajesTxt.setColumns(25);
        mensajesTxt.setRows(5);

        jScrollPane1.setViewportView(mensajesTxt);

        getContentPane().add(jScrollPane1);
        jScrollPane1.setBounds(20, 160, 410, 70);

        setSize(new java.awt.Dimension(491, 290));
        setLocationRelativeTo(null);
    }// </editor-fold>

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new PrincipalSrv().setVisible(true);
            }
        });

    }
    private void bIniciarActionPerformed(java.awt.event.ActionEvent evt) {
        iniciarServidor();
    }

    private void iniciarServidor() {
        new Thread(new Runnable() {
            public void run() {
                try {
                    InetAddress addr = InetAddress.getLocalHost();
                    serverSocket = new ServerSocket( PORT);
                    mensajesTxt.append("Servidor TCP en ejecución: "+ addr + " ,Puerto " + serverSocket.getLocalPort()+ "\n");
                    while (running) {
                        try {
                            Socket clientSocket = serverSocket.accept();
                            if (!running) {
                                break;
                            }
                            ClientHandler newClient = new ClientHandler(clientSocket);
                            new Thread(newClient).start();
                        } catch (IOException e) {
                            if (running) {
                                System.err.println("Error en el servidor: " + e.getMessage());
                            }
                        }
                    }
                } catch (IOException ex) {
                    ex.printStackTrace();
                    mensajesTxt.append("Error en el servidor: " + ex.getMessage() + "\n");
                } finally {
                    detenerServidor();
                }
            }
        }).start();
    }

    private void detenerServidor() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            for (ClientHandler handler : clientSockets.values()) {
                handler.close();
            }
        } catch (IOException ex) {
            System.err.println("Error al cerrar el servidor: " + ex.getMessage());
        }
    }

    private class ClientHandler implements Runnable {
        private Socket clientSocket;
        private BufferedReader in;
        private PrintWriter out;
        private String id;
        private boolean notifiedNickname = false;

        public ClientHandler(Socket clientSocket) throws IOException {
            this.clientSocket = clientSocket;
            this.in = new BufferedReader(new InputStreamReader(this.clientSocket.getInputStream()));
            this.out = new PrintWriter(clientSocket.getOutputStream(), true);
        }

        @Override
        public void run() {
            try {
                String linea;
                while ((linea = in.readLine()) != null) {
                    if (!notifiedNickname) {
                        this.notifiedNickname = true;
                        this.id = linea;
                        clientSockets.put(linea, this);
                    } else {
                        String[] parts = linea.split("\\&");
                        if (parts.length > 0) {
                            switch (parts[0]) {
                                case "all" :
                                    for (Map.Entry<String, ClientHandler> client : clientSockets.entrySet()) {
                                        if (client.getValue().out != null) {
                                            if (Objects.equals(client.getKey(), id)) {
                                                client.getValue().out.println("Tú: " + parts[1]);
                                            } else {
                                                client.getValue().out.println(id + " dice: " + parts[1]);
                                            }
                                        }
                                    }
                                    break;
                                case "custom":
                                    for (Map.Entry<String, ClientHandler> client : clientSockets.entrySet()) {
                                        if (client.getValue().out != null) {
                                            if (Objects.equals(client.getKey(), parts[1])) {
                                                if (Objects.equals(client.getKey(), id)) {
                                                    client.getValue().out.println("Tú: " + parts[2]);
                                                } else {
                                                    client.getValue().out.println(id + " dice: " + parts[2]);
                                                }
                                            }
                                        }
                                    }
                                    break;
                                case "clients":
                                    StringBuilder sb = new StringBuilder();
                                    for (Map.Entry<String, ClientHandler> client : clientSockets.entrySet()) {
                                        if (sb.length() > 0) {
                                            sb.append(",");
                                        }
                                        sb.append(client.getKey());
                                    }
                                    String clients = "clients&" + sb;
                                    for (Map.Entry<String, ClientHandler> client : clientSockets.entrySet()) {
                                        if (client.getValue().out != null) {
                                                client.getValue().out.println(clients);
                                        }
                                    }
                                    break;
                            }
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void close() {
            try {
                if (clientSocket != null && !clientSocket.isClosed()) {
                    clientSocket.close();
                }
            } catch (IOException ex) {
                System.err.println("Error al cerrar el socket del cliente: " + ex.getMessage());
            }
        }
    }
}
