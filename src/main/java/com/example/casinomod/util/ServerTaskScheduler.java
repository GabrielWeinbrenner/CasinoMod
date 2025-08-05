package com.example.casinomod.util;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import net.minecraft.server.MinecraftServer;

public class ServerTaskScheduler {
  private static final Queue<ScheduledTask> tasks = new ConcurrentLinkedQueue<>();

  public static void schedule(MinecraftServer server, Runnable task, int delayTicks) {
    tasks.add(new ScheduledTask(server.getTickCount() + delayTicks, task));
  }

  public static void tick(int currentTick) {
    ScheduledTask task;
    while ((task = tasks.peek()) != null) {
      if (task.executeAt <= currentTick) {
        tasks.poll();
        try {
          task.runnable.run();
        } catch (Exception e) {
          e.printStackTrace();
        }
      } else {
        break;
      }
    }
  }

  private static class ScheduledTask {
    final int executeAt;
    final Runnable runnable;

    ScheduledTask(int executeAt, Runnable runnable) {
      this.executeAt = executeAt;
      this.runnable = runnable;
    }
  }
}
