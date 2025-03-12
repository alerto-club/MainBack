package ru.alerto.proxyback.controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.alerto.proxyback.databases.objects.DirectoryOrFile;
import ru.alerto.proxyback.databases.repositories.DirectoryOrFileRepository;
import ru.alerto.proxyback.databases.repositories.UserRepository;
import ru.alerto.proxyback.requests.Directory;
import ru.alerto.proxyback.requests.GateRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping({"/api/v1"})
public class MainController {

    private final UserRepository userRepository;
    private final DirectoryOrFileRepository directoryOrFileRepository;

    public MainController(UserRepository userRepository, DirectoryOrFileRepository directoryOrFileRepository) {
        this.userRepository = userRepository;
        this.directoryOrFileRepository = directoryOrFileRepository;
    }

    @GetMapping("/start")
    public GateRequest start(@RequestParam Long tgId) {
        GateRequest gateRequest = new GateRequest();

        List<Directory> dirs = new ArrayList<>();

        userRepository.findByTgId(tgId).ifPresent(user -> {
            Optional<DirectoryOrFile> directoryOpt = directoryOrFileRepository.findById(directoryOrFileRepository.findByPath("./files/"  + user.getYear() + "/" + user.getDirection()).get().getId());
            if (directoryOpt.isPresent()) {
                for (DirectoryOrFile child : directoryOrFileRepository.findDirectChildren(directoryOpt.get().getPath())) {
                    String[] directoryTitle = child.getPath().split("/");
                    dirs.add(new Directory(directoryTitle[directoryTitle.length - 1], child.getId()));
                }
            }
        });

        gateRequest.setTgId(tgId);
        gateRequest.setDirs(dirs);
        gateRequest.setCorrectDirectory("You on the home page");

        return gateRequest;
    }

    @GetMapping("/gate")
    public GateRequest gate(@RequestParam Long tgId, @RequestParam Long directoryId) {
        GateRequest gateRequest = new GateRequest();

        List<Directory> dirs = new ArrayList<>();

        directoryOrFileRepository.findById(directoryId).ifPresent(directoryOrFileArch -> {
            int index = findNthOccurrence(directoryOrFileArch.getPath(), '/', 4);

            if (index == -1) {
                gateRequest.setCorrectDirectory("You on the home page");
            }
            else{
                gateRequest.setCorrectDirectory("You are in: " + directoryOrFileArch.getPath().substring(index + 1).replace("/", " -> "));
            }

            Optional<DirectoryOrFile> directoryOpt = directoryOrFileRepository.findById(directoryOrFileRepository.findByPath(directoryOrFileArch.getPath()).get().getId());
            if (directoryOpt.isPresent()) {
                for (DirectoryOrFile child : directoryOrFileRepository.findDirectChildren(directoryOpt.get().getPath())) {
                    String[] directoryTitle = child.getPath().split("/");
                    dirs.add(new Directory(directoryTitle[directoryTitle.length - 1], child.getId()));
                }
            }
        });

        gateRequest.setTgId(tgId);
        gateRequest.setDirs(dirs);

        return gateRequest;
    }

    public static int findNthOccurrence(String str, char ch, int n) {
        int count = 0;
        for (int i = 0; i < str.length(); i++) {
            if (str.charAt(i) == ch) {
                count++;
                if (count == n) {
                    return i;
                }
            }
        }
        return -1;
    }

}