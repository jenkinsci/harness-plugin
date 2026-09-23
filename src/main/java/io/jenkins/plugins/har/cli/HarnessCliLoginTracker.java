package io.jenkins.plugins.har.cli;

import hudson.model.InvisibleAction;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Build action that records which agents have already run {@code hc auth login}
 * for this build.
 *
 * <p>Auth state for the Harness CLI lives on each agent (e.g. {@code ~/.harness/auth.json}),
 * so login must be tracked per node — not once for the whole {@link hudson.model.Run}.
 *
 * <p>Also stores the binary path and workspace used on each agent so logout can
 * run on every agent that logged in.
 */
public class HarnessCliLoginTracker extends InvisibleAction {

    private static final long serialVersionUID = 2L;

    /** nodeName → session details from login time. */
    private final Map<String, AgentSession> sessions = new LinkedHashMap<>();

    /** Transient — not persisted; reset to false after a Jenkins restart (acceptable). */
    private transient volatile boolean loggedOut;

    public HarnessCliLoginTracker() {
    }

    /**
     * @return {@code true} if this agent has not yet logged in for this build
     */
    public synchronized boolean needsLogin(String nodeName) {
        return !sessions.containsKey(StringUtils.defaultString(nodeName));
    }

    /**
     * Records a successful login on the given agent.
     */
    public synchronized void recordLogin(String hcBinaryPath, String workspacePath, String nodeName) {
        String key = StringUtils.defaultString(nodeName);
        sessions.put(key, new AgentSession(hcBinaryPath, workspacePath, key));
    }

    /**
     * @return immutable snapshot of agents that logged in during this build
     */
    public synchronized Collection<AgentSession> getSessions() {
        return Collections.unmodifiableList(new ArrayList<>(sessions.values()));
    }

    /**
     * Claims the logout responsibility. Returns {@code true} exactly once;
     * subsequent calls return {@code false} so that neither the Disposer nor
     * the RunListener runs logout twice on the same build.
     */
    public synchronized boolean markLoggedOut() {
        if (loggedOut) {
            return false;
        }
        loggedOut = true;
        return true;
    }

    /**
     * Login details captured for one agent.
     */
    public static final class AgentSession implements Serializable {
        private static final long serialVersionUID = 1L;

        private final String hcBinaryPath;
        private final String workspacePath;
        private final String nodeName;

        public AgentSession(String hcBinaryPath, String workspacePath, String nodeName) {
            this.hcBinaryPath = hcBinaryPath;
            this.workspacePath = workspacePath;
            this.nodeName = nodeName;
        }

        public String getHcBinaryPath()  { return hcBinaryPath; }
        public String getWorkspacePath() { return workspacePath; }
        public String getNodeName()      { return nodeName; }
    }
}
