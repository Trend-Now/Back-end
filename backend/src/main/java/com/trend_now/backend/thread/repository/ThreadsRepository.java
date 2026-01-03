package com.trend_now.backend.thread.repository;

import com.trend_now.backend.thread.domain.Threads;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ThreadsRepository extends JpaRepository<Threads, Long> {

}
