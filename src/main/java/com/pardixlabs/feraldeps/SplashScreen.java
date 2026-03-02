package com.pardixlabs.feraldeps;

import javax.swing.*;
import java.awt.*;
import java.net.URL;

public class SplashScreen extends JWindow {
    private static final int DISPLAY_TIME = 3000; // 3 seconds

    public SplashScreen() {
        // Load splash image
        URL imageURL = getClass().getClassLoader().getResource("splash.png");
        
        if (imageURL != null) {
            ImageIcon splashIcon = new ImageIcon(imageURL);
            JLabel splashLabel = new JLabel(splashIcon);
            getContentPane().add(splashLabel, BorderLayout.CENTER);
            
            pack();
        } else {
            // Fallback if image not found
            JPanel panel = new JPanel(new BorderLayout());
            panel.setPreferredSize(new Dimension(500, 300));
            panel.setBackground(new Color(45, 52, 54));
            
            JLabel titleLabel = new JLabel("FeralDeps", SwingConstants.CENTER);
            titleLabel.setFont(new Font("Arial", Font.BOLD, 48));
            titleLabel.setForeground(Color.WHITE);
            
            JLabel subtitleLabel = new JLabel("Dependency Scanner", SwingConstants.CENTER);
            subtitleLabel.setFont(new Font("Arial", Font.PLAIN, 20));
            subtitleLabel.setForeground(new Color(200, 200, 200));
            
            JPanel textPanel = new JPanel(new GridLayout(2, 1));
            textPanel.setBackground(new Color(45, 52, 54));
            textPanel.add(titleLabel);
            textPanel.add(subtitleLabel);
            
            panel.add(textPanel, BorderLayout.CENTER);
            getContentPane().add(panel);
            pack();
        }
        
        setLocationRelativeTo(null);
    }

    public void showSplash() {
        setVisible(true);
        
        // Auto-close after delay
        Timer timer = new Timer(DISPLAY_TIME, e -> {
            setVisible(false);
            dispose();
        });
        timer.setRepeats(false);
        timer.start();
    }

    public static void show(Runnable onComplete) {
        SwingUtilities.invokeLater(() -> {
            SplashScreen splash = new SplashScreen();
            splash.showSplash();
            
            // Wait for splash to finish, then run the completion callback
            Timer timer = new Timer(DISPLAY_TIME, e -> onComplete.run());
            timer.setRepeats(false);
            timer.start();
        });
    }
}
