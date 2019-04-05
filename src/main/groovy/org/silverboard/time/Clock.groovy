package org.silverboard.time

import java.net.InetAddress
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.GraphicsDevice
import java.awt.GraphicsEnvironment
import java.awt.Rectangle
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.SwingConstants
import java.awt.Font
import javax.swing.plaf.FontUIResource
import org.apache.commons.net.ntp.NTPUDPClient
import org.apache.commons.net.ntp.NtpV3Packet
import org.apache.commons.net.ntp.TimeInfo

class Clock {

    // List of time servers: http://tf.nist.gov/tf-cgi/servers.cgi
    // Do not query time server more than once every 4 seconds
    private static final String TIME_SERVER = 'time-d-g.nist.gov'

    private static final int MINUTE_IN_MILLIS = 60000

    private Date time
    private JFrame jFrame
    private JLabel timeLabel

    /*
     The clock takes its initial setting from NIST and resynchronizes every 15 minutes.
     */
    static void main(String [] args) {
        Clock clockInstance = new Clock()
        clockInstance.initClock()
        Thread.sleep(clockInstance.millisToNextMinue())

        while(true) {
            Integer waitTime = clockInstance.syncTime()
            clockInstance.update()
            Thread.sleep(waitTime)
        }
    }

    void initClock() {
        initializeWindow()
        syncNistTime()
        update()
    }

    void syncNistTime() {
        NTPUDPClient timeClient = new NTPUDPClient()
        InetAddress inetAddress = InetAddress.getByName(TIME_SERVER)
        TimeInfo timeInfo = timeClient.getTime(inetAddress)
        NtpV3Packet message = timeInfo.getMessage()
        long serverTime = message.getTransmitTimeStamp().getTime()
        time = new Date(serverTime)
    }

    void initializeWindow() {
        jFrame = new JFrame('Atomic Time')
        jFrame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        jFrame.getContentPane().setBackground(Color.BLACK)
        jFrame.setSize(300, 100)
        timeLabel = new JLabel('', SwingConstants.CENTER)
        timeLabel.preferredSize = new Dimension(300, 100)
        timeLabel.font = new FontUIResource('System 12pt', Font.PLAIN, 18)
        timeLabel.setForeground(Color.GREEN)
        jFrame.contentPane.add(timeLabel, BorderLayout.CENTER)
        jFrame.pack()
        // Position lower right
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment()
        GraphicsDevice defaultScreen = ge.getDefaultScreenDevice()
        Rectangle rect = defaultScreen.getDefaultConfiguration().getBounds()
        int x = (int) rect.getMaxX() - jFrame.getWidth()
        int y = (int) rect.getMaxY() - jFrame.getHeight() - 100
        jFrame.setLocation(x, y);
        jFrame.visible = true
    }

    void update() {
        timeLabel.setText(time.format('M/d/yyyy h:mm a z'))
    }

    Integer syncTime() {
        Integer waitTime
        Calendar calendar = GregorianCalendar.getInstance()
        calendar.setTime(time)
        Integer minutes = calendar.get(Calendar.MINUTE)
        if (minutes % 15 == 0) {
            time = getNistTime()
            waitTime = millisToNextMinute()
        } else {
            calendar.add(Calendar.MINUTE, 1)
            time = calendar.time
            waitTime = MINUTE_IN_MILLIS
        }
        return waitTime
    }

    Integer millisToNextMinue() {
        Calendar calendar = GregorianCalendar.getInstance()
        calendar.setTime(time)
        Integer seconds = calendar.get(Calendar.SECOND)
        Integer milliseconds = calendar.get(Calendar.MILLISECOND)
        Integer waitTime = ((60 - seconds) * 1000) + (1000 - milliseconds)
        return waitTime
    }
}