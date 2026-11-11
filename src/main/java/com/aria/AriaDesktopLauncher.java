package com.aria;

import java.nio.file.*;
import java.util.*;

/** IntelliJ's green Run invokes the same safe Windows launcher as ARIA.cmd; no alternate DB profile. */
public final class AriaDesktopLauncher {
 private AriaDesktopLauncher(){}
 public static void main(String[] args)throws Exception{
  Path root=Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
  for(int n=0;n<5;n++){
   if(Files.isRegularFile(root.resolve("scripts/desktop/start.ps1")))break;
   if(Files.isRegularFile(root.resolve("backend/scripts/desktop/start.ps1"))){root=root.resolve("backend");break;}
   root=root.getParent();if(root==null)throw new IllegalStateException("Откройте существующий проект ARIA");
  }
  var command=new ArrayList<>(List.of("powershell.exe","-NoProfile","-ExecutionPolicy","Bypass","-File",root.resolve("scripts/desktop/start.ps1").toString()));
  for(String arg:args){if(arg.equals("--no-browser"))command.add("-NoBrowser");else if(arg.equals("--skip-build"))command.add("-SkipBuild");else throw new IllegalArgumentException("Неизвестный параметр запуска ARIA");}
  int status=new ProcessBuilder(command).directory(root.toFile()).inheritIO().start().waitFor();if(status!=0)throw new IllegalStateException("Не удалось запустить ARIA. Подробности — в data/desktop/start.log");
 }
}
