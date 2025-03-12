package ru.alerto.proxyback.databases.repositories;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import ru.alerto.proxyback.databases.objects.DirectoryOrFile;

import java.util.List;
import java.util.Optional;

public interface DirectoryOrFileRepository extends CrudRepository<DirectoryOrFile, Long>{

    Optional<DirectoryOrFile> findById(Long id);

    Optional<DirectoryOrFile> findByPath(String path);

    @Query("SELECT d FROM DirectoryOrFile d WHERE d.path LIKE CONCAT(:parentPath, '/%') AND d.path NOT LIKE CONCAT(:parentPath, '/%/%')")
    List<DirectoryOrFile> findDirectChildren(@Param("parentPath") String parentPath);
}