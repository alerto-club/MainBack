package ru.alerto.proxyback.databases.repositories;

import org.springframework.data.repository.CrudRepository;
import ru.alerto.proxyback.databases.objects.User;

import java.util.Optional;

public interface UserRepository extends CrudRepository<User, Long>{

    Optional<User> findByTgId(Long id);

}
