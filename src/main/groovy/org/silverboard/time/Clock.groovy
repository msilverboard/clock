package org.silverboard.time

import java.net.InetAddress
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.Font
import java.awt.GraphicsDevice
import java.awt.GraphicsEnvironment
import java.awt.Rectangle
import javax.swing.ImageIcon
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.SwingConstants
import javax.swing.plaf.FontUIResource
import org.apache.commons.net.ntp.NTPUDPClient
import org.apache.commons.net.ntp.NtpV3Packet
import org.apache.commons.net.ntp.TimeInfo

// List of time servers: http://tf.nist.gov/tf-cgi/servers.cgi
// Do not query time server more than once every 4 seconds
class Clock {

    private static final int MINUTE_IN_MILLIS = 60 * 000
    private static final int HOUR_IN_MILLIS = 60 * MINUTE_IN_MILLIS
    private static final List<String> STRING_PROPS = ['timeServer','format', 'fontName']
    private static final List<String> INT_PROPS = ['windowWidth', 'windowHeight', 'backgroundColorR',
        'backgroundColorG', 'backgroundColorB', 'foregroundColorR', 'foregroundColorG', 
        'foregroundColorB', 'fontStyle', 'fontSize']

    private Date currentTime
    private Date lastSync
    private JFrame jFrame
    private JLabel timeLabel

    private String timeServer = 'time-d-g.nist.gov'
    private String format = 'M/d/yyyy h:mm a z'
    private String fontName = 'System 12pt'
    private int fontStyle = Font.PLAIN
    private int fontSize = 18
    private int windowWidth = 300
    private int windowHeight = 100
    private int backgroundColorR = 0
    private int backgroundColorG = 0
    private int backgroundColorB = 0
    private int foregroundColorR = 0
    private int foregroundColorG = 128
    private int foregroundColorB = 0

    Clock() {
        initClock()
    }

    static void main(String [] args) {
        Clock clockInstance = new Clock()
        Thread.sleep(clockInstance.millisToNextMinute())

        while(true) {
            Integer waitTime = clockInstance.syncTime()
            clockInstance.update()
            Thread.sleep(waitTime)
        }
    }

    void initClock() {
        readProperties()
        initializeWindow()
        syncNistTime()
        update()
    }

    void readProperties() {
        Properties props = new Properties()
        String userHome = System.getProperty('user.home')
        File file = new File("${userHome}/clock.properties")
        if (file.exists()) {
            props.load(file.newInputStream())
        }

        STRING_PROPS.each { propName ->
            if (props && props[propName] != null) {
                this[propName] = props[propName]
            }
        }

        INT_PROPS.each { propName ->
            if (props && props[propName] != null) {
                this[propName] = Integer.parseInt(props[propName])
            }
        }
    }

    void syncNistTime() {
        NTPUDPClient timeClient = new NTPUDPClient()
        InetAddress inetAddress = InetAddress.getByName(timeServer)
        TimeInfo timeInfo = timeClient.getTime(inetAddress)
        NtpV3Packet message = timeInfo.getMessage()
        long serverTime = message.getTransmitTimeStamp().getTime()
        currentTime = new Date(serverTime)
        lastSync = new Date(serverTime)
    }

    void initializeWindow() {
        URL iconUrl = getClass().getResource('/modclock32.png')
        ImageIcon icon = new ImageIcon(iconUrl)
        Color backgroundColor = new Color(
            backgroundColorR,
            backgroundColorG,
            backgroundColorB)
        Color foregroundColor = new Color(
            foregroundColorR,
            foregroundColorG,
            foregroundColorB)
        jFrame = new JFrame('Atomic Time')
        jFrame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        jFrame.getContentPane().setBackground(backgroundColor)
        jFrame.setSize(300, 100)
        jFrame.setIconImage(icon.image)
        timeLabel = new JLabel('', SwingConstants.CENTER)
        timeLabel.preferredSize = new Dimension(windowWidth, windowHeight)
        timeLabel.font = new FontUIResource(fontName, fontStyle, fontSize)
        timeLabel.setForeground(foregroundColor)
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
        timeLabel.setText(currentTime.format(format))
    }

    Integer syncTime() {
        Integer waitTime
        Calendar calendar = GregorianCalendar.getInstance()
        calendar.setTime(currentTime)
        Integer minutes = calendar.get(Calendar.MINUTE)
        if (currentTime.time > (lastSync.time + (12 * HOUR_IN_MILLIS))) {
            syncNistTime()
            waitTime = millisToNextMinute()
        } else {
            calendar.add(Calendar.MINUTE, 1)
            currentTime = calendar.time
            waitTime = MINUTE_IN_MILLIS
        }
        return waitTime
    }

    Long millisToNextMinute() {
        Calendar calendar = GregorianCalendar.getInstance()
        calendar.setTime(currentTime)
        Integer seconds = calendar.get(Calendar.SECOND)
        Integer milliseconds = calendar.get(Calendar.MILLISECOND)
        Integer waitTime = ((60 - seconds) * 1000) + (1000 - milliseconds)
        Long result = new Long(waitTime.intValue())
        return result
    }
}