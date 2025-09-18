package com.example.app.email.repository;

import com.example.app.email.entity.TokenType;

import com.example.app.user.entity.User;
import com.example.app.email.entity.TokenType;
import com.example.app.email.entity.VerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** Repository for managing verification tokens */
@Repository
public interface VerificationTokenRepository extends JpaRepository<VerificationToken, Long> {

  /** Find token by token string */
  Optional<VerificationToken> findByToken(String token);

  /** Find valid token by token string */
  @Query(
      "SELECT t FROM VerificationToken t WHERE t.token = :token AND t.used = false AND t.expiresAt > :now")
  Optional<VerificationToken> findValidToken(
      @Param("token") String token, @Param("now") LocalDateTime now);

  /** Find tokens by user and type */
  List<VerificationToken> findByUserAndTokenType(User user, TokenType tokenType);

  /** Find valid tokens by user and type */
  @Query(
      "SELECT t FROM VerificationToken t WHERE t.user = :user AND t.tokenType = :tokenType "
          + "AND t.used = false AND t.expiresAt > :now")
  List<VerificationToken> findValidTokensByUserAndType(
      @Param("user") User user,
      @Param("tokenType") TokenType tokenType,
      @Param("now") LocalDateTime now);

  /** Find token by user, email, and type */
  Optional<VerificationToken> findByUserAndEmailAndTokenType(
      User user, String email, TokenType tokenType);

  /** Find expired tokens */
  @Query("SELECT t FROM VerificationToken t WHERE t.expiresAt < :now")
  List<VerificationToken> findExpiredTokens(@Param("now") LocalDateTime now);

  /** Delete expired tokens */
  @Modifying
  @Transactional
  @Query("DELETE FROM VerificationToken t WHERE t.expiresAt < :now")
  int deleteExpiredTokens(@Param("now") LocalDateTime now);

  /** Delete used tokens older than specified date */
  @Modifying
  @Transactional
  @Query("DELETE FROM VerificationToken t WHERE t.used = true AND t.confirmedAt < :cutoffDate")
  int deleteUsedTokensOlderThan(@Param("cutoffDate") LocalDateTime cutoffDate);

  /** Invalidate all existing tokens for user and type */
  @Modifying
  @Transactional
  @Query(
      "UPDATE VerificationToken t SET t.used = true WHERE t.user = :user AND t.tokenType = :tokenType AND t.used = false")
  int invalidateTokensForUser(@Param("user") User user, @Param("tokenType") TokenType tokenType);

  /** Count tokens created in last time period for rate limiting */
  @Query(
      "SELECT COUNT(t) FROM VerificationToken t WHERE t.user = :user AND t.tokenType = :tokenType "
          + "AND t.createdAt > :since")
  long countTokensCreatedSince(
      @Param("user") User user,
      @Param("tokenType") TokenType tokenType,
      @Param("since") LocalDateTime since);

  /** Count tokens created for email in last time period */
  @Query(
      "SELECT COUNT(t) FROM VerificationToken t WHERE t.email = :email AND t.tokenType = :tokenType "
          + "AND t.createdAt > :since")
  long countTokensCreatedForEmailSince(
      @Param("email") String email,
      @Param("tokenType") TokenType tokenType,
      @Param("since") LocalDateTime since);

  /** Find most recent token for user and type */
  @Query(
      "SELECT t FROM VerificationToken t WHERE t.user = :user AND t.tokenType = :tokenType "
          + "ORDER BY t.createdAt DESC")
  List<VerificationToken> findMostRecentTokensByUserAndType(
      @Param("user") User user, @Param("tokenType") TokenType tokenType);
}
