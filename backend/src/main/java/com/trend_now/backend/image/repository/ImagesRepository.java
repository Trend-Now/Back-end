package com.trend_now.backend.image.repository;

import com.trend_now.backend.image.domain.Images;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ImagesRepository extends JpaRepository<Images, Long> {

    @Query("SELECT i.s3key FROM Images i WHERE i.posts.id = :postId")
    List<String> findS3KeyByPostsId(Long postId);

    @Query("SELECT i.s3key FROM Images i WHERE i.id IN :ids")
    List<String> findS3KeyByIdIn(List<Long> ids);

    @Query("SELECT i.s3key FROM Images i WHERE i.id = :id")
    String findS3KeyById(Long id);

    @Query("SELECT i.s3key FROM Images i WHERE i.posts IS NULL")
    List<String> findS3KeyByPostsIsNull();

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM Images i WHERE i.id IN :ids")
    void deleteAllByIdIn(List<Long> ids);

    void deleteAllByPosts_Id(Long postsId);

    List<Images> findAllByPosts_Id(Long postsId);

    @Modifying
    @Query("DELETE FROM Images i WHERE i.posts IS NULL")
    void deleteByPostsIsNull();
}
