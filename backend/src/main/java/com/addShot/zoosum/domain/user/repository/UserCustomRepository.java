package com.addShot.zoosum.domain.user.repository;

import com.addShot.zoosum.entity.User;

public interface UserCustomRepository {

    /**
     * userId를 통해 User Entity를 조회한다.
     * @param userId 사용자 ID
     * @return User Entity
     */
    User findUser(String userId);

    /**
     * nickname을 통해 User Entitu를 조회한다.
     * @param nickname 사용자 닉네임
     * @return User Entity
     */
    User findUserByNickname(String nickname);
}