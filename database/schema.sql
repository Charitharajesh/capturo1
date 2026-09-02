-- phpMyAdmin SQL Dump
-- version 5.2.1
-- https://www.phpmyadmin.net/
--
-- Host: 127.0.0.1
-- Generation Time: Jun 01, 2026 at 03:08 AM
-- Server version: 10.4.32-MariaDB
-- PHP Version: 8.2.12

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Database: `capturo`
--
CREATE DATABASE IF NOT EXISTS `capturo` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `capturo`;

-- --------------------------------------------------------

--
-- Table structure for table `ai_caption_cache`
--
-- Creation: Jun 01, 2026 at 12:00 AM
--

DROP TABLE IF EXISTS `ai_caption_cache`;
CREATE TABLE `ai_caption_cache` (
  `id` char(36) NOT NULL DEFAULT uuid(),
  `gallery_item_id` char(36) NOT NULL,
  `creator_id` char(36) NOT NULL,
  `caption` varchar(200) NOT NULL,
  `tags` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL CHECK (json_valid(`tags`)),
  `event_type` varchar(60) NOT NULL,
  `location` varchar(200) NOT NULL,
  `model_used` varchar(60) NOT NULL,
  `was_edited` tinyint(1) NOT NULL DEFAULT 0,
  `created_at` datetime NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- RELATIONSHIPS FOR TABLE `ai_caption_cache`:
--   `creator_id`
--       `users` -> `id`
--   `gallery_item_id`
--       `gallery_items` -> `id`
--

-- --------------------------------------------------------

--
-- Table structure for table `ai_insights_cache`
--
-- Creation: Jun 01, 2026 at 12:00 AM
--

DROP TABLE IF EXISTS `ai_insights_cache`;
CREATE TABLE `ai_insights_cache` (
  `id` char(36) NOT NULL DEFAULT uuid(),
  `creator_id` char(36) NOT NULL,
  `trend` enum('up','down','stable') NOT NULL,
  `trend_pct` decimal(5,2) NOT NULL DEFAULT 0.00,
  `forecast_next_month_inr` int(11) NOT NULL DEFAULT 0,
  `top_insight` text NOT NULL,
  `recommendations` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL CHECK (json_valid(`recommendations`)),
  `stats_snapshot` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL CHECK (json_valid(`stats_snapshot`)),
  `model_used` varchar(60) NOT NULL,
  `generated_at` datetime NOT NULL DEFAULT current_timestamp(),
  `updated_at` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- RELATIONSHIPS FOR TABLE `ai_insights_cache`:
--   `creator_id`
--       `users` -> `id`
--

-- --------------------------------------------------------

--
-- Table structure for table `ai_review_summaries`
--
-- Creation: Jun 01, 2026 at 12:00 AM
--

DROP TABLE IF EXISTS `ai_review_summaries`;
CREATE TABLE `ai_review_summaries` (
  `id` char(36) NOT NULL DEFAULT uuid(),
  `creator_id` char(36) NOT NULL,
  `review_count` smallint(6) NOT NULL,
  `summary` text NOT NULL,
  `highlights` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL CHECK (json_valid(`highlights`)),
  `watch_out` text DEFAULT NULL,
  `sentiment_score` decimal(4,3) NOT NULL DEFAULT 0.000,
  `model_used` varchar(60) NOT NULL,
  `prompt_tokens` smallint(6) NOT NULL DEFAULT 0,
  `completion_tokens` smallint(6) NOT NULL DEFAULT 0,
  `generated_at` datetime NOT NULL DEFAULT current_timestamp(),
  `updated_at` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- RELATIONSHIPS FOR TABLE `ai_review_summaries`:
--   `creator_id`
--       `users` -> `id`
--

-- --------------------------------------------------------

--
-- Table structure for table `ai_search_logs`
--
-- Creation: Jun 01, 2026 at 12:00 AM
--

DROP TABLE IF EXISTS `ai_search_logs`;
CREATE TABLE `ai_search_logs` (
  `id` char(36) NOT NULL DEFAULT uuid(),
  `user_id` char(36) NOT NULL,
  `raw_query` text NOT NULL,
  `parsed_filters` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL CHECK (json_valid(`parsed_filters`)),
  `model_used` varchar(60) NOT NULL,
  `prompt_tokens` smallint(6) NOT NULL DEFAULT 0,
  `completion_tokens` smallint(6) NOT NULL DEFAULT 0,
  `cost_usd` decimal(10,6) NOT NULL DEFAULT 0.000000,
  `duration_ms` smallint(6) NOT NULL DEFAULT 0,
  `cache_hit` tinyint(1) NOT NULL DEFAULT 0,
  `success` tinyint(1) NOT NULL DEFAULT 1,
  `created_at` datetime NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- RELATIONSHIPS FOR TABLE `ai_search_logs`:
--   `user_id`
--       `users` -> `id`
--

-- --------------------------------------------------------

--
-- Table structure for table `ai_usage_logs`
--
-- Creation: May 31, 2026 at 06:43 PM
--

DROP TABLE IF EXISTS `ai_usage_logs`;
CREATE TABLE `ai_usage_logs` (
  `id` varchar(36) NOT NULL,
  `user_id` varchar(36) NOT NULL,
  `feature` varchar(100) NOT NULL,
  `model` varchar(100) NOT NULL,
  `prompt_tokens` int(11) NOT NULL,
  `completion_tokens` int(11) NOT NULL,
  `cost_usd` decimal(10,6) NOT NULL,
  `duration_ms` int(11) NOT NULL,
  `cache_hit` tinyint(1) NOT NULL,
  `created_at` datetime NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- RELATIONSHIPS FOR TABLE `ai_usage_logs`:
--   `user_id`
--       `users` -> `id`
--

-- --------------------------------------------------------

--
-- Table structure for table `alembic_version`
--
-- Creation: May 31, 2026 at 06:43 PM
--

DROP TABLE IF EXISTS `alembic_version`;
CREATE TABLE `alembic_version` (
  `version_num` varchar(32) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- RELATIONSHIPS FOR TABLE `alembic_version`:
--

-- --------------------------------------------------------

--
-- Table structure for table `bookings`
--
-- Creation: May 30, 2026 at 11:37 AM
-- Last update: Jun 01, 2026 at 01:00 AM
--

DROP TABLE IF EXISTS `bookings`;
CREATE TABLE `bookings` (
  `id` char(36) NOT NULL DEFAULT uuid(),
  `attendee_id` char(36) NOT NULL,
  `creator_id` char(36) NOT NULL,
  `event_type` enum('wedding','birthday','corporate','graduation','party','other') NOT NULL,
  `location` varchar(500) NOT NULL,
  `event_date` date NOT NULL,
  `start_time` time NOT NULL,
  `duration_hours` decimal(4,1) NOT NULL,
  `total_amount` decimal(12,2) NOT NULL,
  `status` enum('pending','confirmed','in_progress','completed','cancelled','disputed') NOT NULL DEFAULT 'pending',
  `special_notes` text DEFAULT NULL,
  `invoice_url` varchar(500) DEFAULT NULL,
  `cancelled_by` enum('attendee','creator','admin') DEFAULT NULL,
  `cancellation_reason` text DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT current_timestamp(),
  `updated_at` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  `status_not_cancelled` varchar(50) GENERATED ALWAYS AS (if(`status` <> 'cancelled',`status`,NULL)) VIRTUAL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- RELATIONSHIPS FOR TABLE `bookings`:
--   `attendee_id`
--       `users` -> `id`
--   `creator_id`
--       `users` -> `id`
--

-- --------------------------------------------------------

--
-- Table structure for table `creator_followers`
--
-- Creation: May 31, 2026 at 07:21 PM
--

DROP TABLE IF EXISTS `creator_followers`;
CREATE TABLE `creator_followers` (
  `id` varchar(36) NOT NULL,
  `follower_id` varchar(36) NOT NULL,
  `creator_id` varchar(36) NOT NULL,
  `created_at` datetime NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- RELATIONSHIPS FOR TABLE `creator_followers`:
--   `follower_id`
--       `users` -> `id`
--   `creator_id`
--       `creator_profiles` -> `id`
--

-- --------------------------------------------------------

--
-- Table structure for table `creator_profiles`
--
-- Creation: May 30, 2026 at 12:38 PM
-- Last update: Jun 01, 2026 at 01:00 AM
--

DROP TABLE IF EXISTS `creator_profiles`;
CREATE TABLE `creator_profiles` (
  `id` char(36) NOT NULL DEFAULT uuid(),
  `user_id` char(36) NOT NULL,
  `specializations` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL CHECK (json_valid(`specializations`)),
  `hourly_rate` decimal(10,2) NOT NULL DEFAULT 999.00,
  `minimum_hours` tinyint(4) NOT NULL DEFAULT 2,
  `bio` text DEFAULT NULL,
  `years_experience` tinyint(4) DEFAULT NULL,
  `equipment` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL CHECK (json_valid(`equipment`)),
  `availability_status` enum('available','busy','offline') NOT NULL DEFAULT 'available',
  `avg_rating` decimal(3,2) NOT NULL DEFAULT 0.00,
  `total_reviews` int(11) NOT NULL DEFAULT 0,
  `total_bookings` int(11) NOT NULL DEFAULT 0,
  `on_time_rate` decimal(5,2) NOT NULL DEFAULT 100.00,
  `latitude` decimal(10,8) DEFAULT NULL,
  `longitude` decimal(11,8) DEFAULT NULL,
  `service_radius_km` smallint(6) NOT NULL DEFAULT 10,
  `is_featured` tinyint(1) NOT NULL DEFAULT 0,
  `created_at` datetime NOT NULL DEFAULT current_timestamp(),
  `updated_at` datetime DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- RELATIONSHIPS FOR TABLE `creator_profiles`:
--   `user_id`
--       `users` -> `id`
--

-- --------------------------------------------------------

--
-- Table structure for table `email_otps`
--
-- Creation: May 31, 2026 at 06:43 PM
-- Last update: Jun 01, 2026 at 01:00 AM
--

DROP TABLE IF EXISTS `email_otps`;
CREATE TABLE `email_otps` (
  `id` varchar(36) NOT NULL,
  `user_id` varchar(36) NOT NULL,
  `otp_code` varchar(255) NOT NULL,
  `otp_type` enum('verify_email','reset_pwd') NOT NULL,
  `is_used` tinyint(1) NOT NULL,
  `expires_at` datetime NOT NULL,
  `created_at` datetime NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- RELATIONSHIPS FOR TABLE `email_otps`:
--   `user_id`
--       `users` -> `id`
--

-- --------------------------------------------------------

--
-- Table structure for table `gallery_items`
--
-- Creation: May 30, 2026 at 11:37 AM
-- Last update: Jun 01, 2026 at 01:00 AM
--

DROP TABLE IF EXISTS `gallery_items`;
CREATE TABLE `gallery_items` (
  `id` char(36) NOT NULL DEFAULT uuid(),
  `creator_id` char(36) NOT NULL,
  `booking_id` char(36) DEFAULT NULL,
  `file_url` varchar(500) NOT NULL,
  `thumbnail_url` varchar(500) DEFAULT NULL,
  `file_type` enum('photo','video') NOT NULL,
  `file_size_bytes` bigint(20) NOT NULL,
  `title` varchar(200) DEFAULT NULL,
  `description` text DEFAULT NULL,
  `is_portfolio` tinyint(1) NOT NULL DEFAULT 0,
  `is_client_delivery` tinyint(1) NOT NULL DEFAULT 0,
  `views_count` int(11) NOT NULL DEFAULT 0,
  `created_at` datetime NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- RELATIONSHIPS FOR TABLE `gallery_items`:
--   `booking_id`
--       `bookings` -> `id`
--   `creator_id`
--       `users` -> `id`
--

-- --------------------------------------------------------

--
-- Table structure for table `messages`
--
-- Creation: May 30, 2026 at 11:37 AM
-- Last update: Jun 01, 2026 at 01:00 AM
--

DROP TABLE IF EXISTS `messages`;
CREATE TABLE `messages` (
  `id` char(36) NOT NULL DEFAULT uuid(),
  `booking_id` char(36) NOT NULL,
  `sender_id` char(36) NOT NULL,
  `receiver_id` char(36) NOT NULL,
  `content` text DEFAULT NULL,
  `message_type` enum('text','image','file','system') NOT NULL DEFAULT 'text',
  `media_url` varchar(500) DEFAULT NULL,
  `is_read` tinyint(1) NOT NULL DEFAULT 0,
  `read_at` datetime DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- RELATIONSHIPS FOR TABLE `messages`:
--   `booking_id`
--       `bookings` -> `id`
--   `receiver_id`
--       `users` -> `id`
--   `sender_id`
--       `users` -> `id`
--

-- --------------------------------------------------------

--
-- Table structure for table `notifications`
--
-- Creation: May 30, 2026 at 11:37 AM
-- Last update: Jun 01, 2026 at 01:00 AM
--

DROP TABLE IF EXISTS `notifications`;
CREATE TABLE `notifications` (
  `id` char(36) NOT NULL DEFAULT uuid(),
  `user_id` char(36) NOT NULL,
  `title` varchar(200) NOT NULL,
  `body` text NOT NULL,
  `notification_type` enum('booking_confirmed','booking_cancelled','new_message','payment_captured','review_requested','creator_accepted','upload_ready') NOT NULL,
  `reference_id` char(36) DEFAULT NULL,
  `reference_type` varchar(50) DEFAULT NULL,
  `is_read` tinyint(1) NOT NULL DEFAULT 0,
  `read_at` datetime DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- RELATIONSHIPS FOR TABLE `notifications`:
--   `user_id`
--       `users` -> `id`
--

-- --------------------------------------------------------

--
-- Table structure for table `payments`
--
-- Creation: May 30, 2026 at 11:37 AM
-- Last update: Jun 01, 2026 at 01:00 AM
--

DROP TABLE IF EXISTS `payments`;
CREATE TABLE `payments` (
  `id` char(36) NOT NULL DEFAULT uuid(),
  `booking_id` char(36) NOT NULL,
  `payer_id` char(36) NOT NULL,
  `amount` decimal(12,2) NOT NULL,
  `refund_amount` decimal(12,2) NOT NULL DEFAULT 0.00,
  `currency` char(3) NOT NULL DEFAULT 'INR',
  `gateway` enum('razorpay','stripe','cash') NOT NULL DEFAULT 'razorpay',
  `gateway_order_id` varchar(100) DEFAULT NULL,
  `gateway_payment_id` varchar(100) DEFAULT NULL,
  `status` enum('pending','authorized','captured','failed','refunded') NOT NULL DEFAULT 'pending',
  `captured_at` datetime DEFAULT NULL,
  `refunded_at` datetime DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- RELATIONSHIPS FOR TABLE `payments`:
--   `booking_id`
--       `bookings` -> `id`
--   `payer_id`
--       `users` -> `id`
--

-- --------------------------------------------------------

--
-- Table structure for table `refresh_tokens`
--
-- Creation: May 31, 2026 at 06:43 PM
--

DROP TABLE IF EXISTS `refresh_tokens`;
CREATE TABLE `refresh_tokens` (
  `id` varchar(36) NOT NULL,
  `user_id` varchar(36) NOT NULL,
  `token` varchar(255) NOT NULL,
  `is_blacklisted` tinyint(1) NOT NULL,
  `expires_at` datetime NOT NULL,
  `created_at` datetime NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- RELATIONSHIPS FOR TABLE `refresh_tokens`:
--   `user_id`
--       `users` -> `id`
--

-- --------------------------------------------------------

--
-- Table structure for table `reviews`
--
-- Creation: May 30, 2026 at 11:37 AM
-- Last update: Jun 01, 2026 at 01:00 AM
--

DROP TABLE IF EXISTS `reviews`;
CREATE TABLE `reviews` (
  `id` char(36) NOT NULL DEFAULT uuid(),
  `booking_id` char(36) NOT NULL,
  `reviewer_id` char(36) NOT NULL,
  `creator_id` char(36) NOT NULL,
  `rating` tinyint(4) NOT NULL,
  `comment` text DEFAULT NULL,
  `is_verified` tinyint(1) NOT NULL DEFAULT 0,
  `created_at` datetime NOT NULL DEFAULT current_timestamp()
) ;

--
-- RELATIONSHIPS FOR TABLE `reviews`:
--   `booking_id`
--       `bookings` -> `id`
--   `creator_id`
--       `users` -> `id`
--   `reviewer_id`
--       `users` -> `id`
--

-- --------------------------------------------------------

--
-- Table structure for table `users`
--
-- Creation: May 30, 2026 at 11:37 AM
-- Last update: Jun 01, 2026 at 01:00 AM
--

DROP TABLE IF EXISTS `users`;
CREATE TABLE `users` (
  `id` char(36) NOT NULL DEFAULT uuid(),
  `full_name` varchar(150) NOT NULL,
  `email` varchar(255) NOT NULL,
  `phone` varchar(20) DEFAULT NULL,
  `hashed_password` varchar(255) NOT NULL,
  `role` enum('attendee','creator','admin') NOT NULL DEFAULT 'attendee',
  `profile_pic_url` varchar(500) DEFAULT NULL,
  `is_active` tinyint(1) NOT NULL DEFAULT 1,
  `is_verified` tinyint(1) NOT NULL DEFAULT 0,
  `fcm_token` varchar(500) DEFAULT NULL,
  `last_login_at` datetime DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT current_timestamp(),
  `updated_at` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- RELATIONSHIPS FOR TABLE `users`:
--

--
-- Indexes for dumped tables
--

--
-- Indexes for table `ai_caption_cache`
--
ALTER TABLE `ai_caption_cache`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `unique_caption_gallery_item_id` (`gallery_item_id`),
  ADD KEY `idx_ai_caption_creator_created` (`creator_id`,`created_at`),
  ADD KEY `idx_ai_caption_was_edited` (`was_edited`);

--
-- Indexes for table `ai_insights_cache`
--
ALTER TABLE `ai_insights_cache`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `unique_insight_creator_id` (`creator_id`),
  ADD KEY `idx_ai_insights_trend_generated` (`trend`,`generated_at`),
  ADD KEY `idx_ai_insights_generated` (`generated_at`);

--
-- Indexes for table `ai_review_summaries`
--
ALTER TABLE `ai_review_summaries`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `unique_creator_id` (`creator_id`),
  ADD KEY `idx_ai_review_count` (`review_count`),
  ADD KEY `idx_ai_review_generated` (`generated_at`);

--
-- Indexes for table `ai_search_logs`
--
ALTER TABLE `ai_search_logs`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_ai_search_user_created` (`user_id`,`created_at`),
  ADD KEY `idx_ai_search_success_created` (`success`,`created_at`),
  ADD KEY `idx_ai_search_cache_hit` (`cache_hit`),
  ADD KEY `idx_ai_search_model` (`model_used`);

--
-- Indexes for table `ai_usage_logs`
--
ALTER TABLE `ai_usage_logs`
  ADD PRIMARY KEY (`id`),
  ADD KEY `user_id` (`user_id`);

--
-- Indexes for table `alembic_version`
--
ALTER TABLE `alembic_version`
  ADD PRIMARY KEY (`version_num`);

--
-- Indexes for table `bookings`
--
ALTER TABLE `bookings`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `unique_creator_booking_slot` (`creator_id`,`event_date`,`start_time`,`status_not_cancelled`),
  ADD KEY `idx_bookings_attendee_status` (`attendee_id`,`status`),
  ADD KEY `idx_bookings_creator_status` (`creator_id`,`status`),
  ADD KEY `idx_bookings_event_date` (`event_date`);

--
-- Indexes for table `creator_followers`
--
ALTER TABLE `creator_followers`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `uq_follower_creator` (`follower_id`,`creator_id`),
  ADD KEY `creator_id` (`creator_id`),
  ADD KEY `idx_followers_lookup` (`follower_id`,`creator_id`);

--
-- Indexes for table `creator_profiles`
--
ALTER TABLE `creator_profiles`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `unique_user_id` (`user_id`),
  ADD KEY `idx_creators_status_rating` (`availability_status`,`avg_rating`),
  ADD KEY `idx_creators_location` (`latitude`,`longitude`),
  ADD KEY `idx_creators_featured` (`is_featured`);

--
-- Indexes for table `email_otps`
--
ALTER TABLE `email_otps`
  ADD PRIMARY KEY (`id`),
  ADD KEY `user_id` (`user_id`);

--
-- Indexes for table `gallery_items`
--
ALTER TABLE `gallery_items`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_gallery_creator_portfolio` (`creator_id`,`is_portfolio`,`created_at`),
  ADD KEY `idx_gallery_booking_delivery` (`booking_id`,`is_client_delivery`),
  ADD KEY `idx_gallery_file_type` (`file_type`);

--
-- Indexes for table `messages`
--
ALTER TABLE `messages`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_messages_booking_created` (`booking_id`,`created_at`),
  ADD KEY `idx_messages_receiver_read` (`receiver_id`,`is_read`),
  ADD KEY `idx_messages_sender` (`sender_id`);

--
-- Indexes for table `notifications`
--
ALTER TABLE `notifications`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_notifications_user_read` (`user_id`,`is_read`,`created_at`),
  ADD KEY `idx_notifications_type` (`notification_type`),
  ADD KEY `idx_notifications_reference` (`reference_id`,`reference_type`);

--
-- Indexes for table `payments`
--
ALTER TABLE `payments`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `unique_booking_id` (`booking_id`),
  ADD UNIQUE KEY `unique_gateway_payment_id` (`gateway_payment_id`),
  ADD KEY `idx_payments_payer_status` (`payer_id`,`status`),
  ADD KEY `idx_payments_status_created` (`status`,`created_at`);

--
-- Indexes for table `refresh_tokens`
--
ALTER TABLE `refresh_tokens`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `ix_refresh_tokens_token` (`token`),
  ADD KEY `user_id` (`user_id`);

--
-- Indexes for table `reviews`
--
ALTER TABLE `reviews`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `unique_booking_review` (`booking_id`),
  ADD KEY `idx_reviews_creator_rating` (`creator_id`,`rating`),
  ADD KEY `idx_reviews_creator_created` (`creator_id`,`created_at`),
  ADD KEY `fk_reviews_reviewer` (`reviewer_id`);

--
-- Indexes for table `users`
--
ALTER TABLE `users`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `unique_email` (`email`),
  ADD KEY `idx_users_role` (`role`),
  ADD KEY `idx_users_active_role` (`is_active`,`role`);

--
-- Constraints for dumped tables
--

--
-- Constraints for table `ai_caption_cache`
--
ALTER TABLE `ai_caption_cache`
  ADD CONSTRAINT `fk_ai_caption_cache_creator` FOREIGN KEY (`creator_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `fk_ai_caption_cache_gallery` FOREIGN KEY (`gallery_item_id`) REFERENCES `gallery_items` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `ai_insights_cache`
--
ALTER TABLE `ai_insights_cache`
  ADD CONSTRAINT `fk_ai_insights_cache_creator` FOREIGN KEY (`creator_id`) REFERENCES `users` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `ai_review_summaries`
--
ALTER TABLE `ai_review_summaries`
  ADD CONSTRAINT `fk_ai_review_summaries_creator` FOREIGN KEY (`creator_id`) REFERENCES `users` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `ai_search_logs`
--
ALTER TABLE `ai_search_logs`
  ADD CONSTRAINT `fk_ai_search_logs_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `ai_usage_logs`
--
ALTER TABLE `ai_usage_logs`
  ADD CONSTRAINT `ai_usage_logs_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `bookings`
--
ALTER TABLE `bookings`
  ADD CONSTRAINT `fk_bookings_attendee` FOREIGN KEY (`attendee_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `fk_bookings_creator` FOREIGN KEY (`creator_id`) REFERENCES `users` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `creator_followers`
--
ALTER TABLE `creator_followers`
  ADD CONSTRAINT `creator_followers_ibfk_1` FOREIGN KEY (`follower_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `creator_followers_ibfk_2` FOREIGN KEY (`creator_id`) REFERENCES `creator_profiles` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `creator_profiles`
--
ALTER TABLE `creator_profiles`
  ADD CONSTRAINT `fk_creator_profiles_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `email_otps`
--
ALTER TABLE `email_otps`
  ADD CONSTRAINT `email_otps_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `gallery_items`
--
ALTER TABLE `gallery_items`
  ADD CONSTRAINT `fk_gallery_booking` FOREIGN KEY (`booking_id`) REFERENCES `bookings` (`id`) ON DELETE SET NULL,
  ADD CONSTRAINT `fk_gallery_creator` FOREIGN KEY (`creator_id`) REFERENCES `users` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `messages`
--
ALTER TABLE `messages`
  ADD CONSTRAINT `fk_messages_booking` FOREIGN KEY (`booking_id`) REFERENCES `bookings` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `fk_messages_receiver` FOREIGN KEY (`receiver_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `fk_messages_sender` FOREIGN KEY (`sender_id`) REFERENCES `users` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `notifications`
--
ALTER TABLE `notifications`
  ADD CONSTRAINT `fk_notifications_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `payments`
--
ALTER TABLE `payments`
  ADD CONSTRAINT `fk_payments_booking` FOREIGN KEY (`booking_id`) REFERENCES `bookings` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `fk_payments_payer` FOREIGN KEY (`payer_id`) REFERENCES `users` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `refresh_tokens`
--
ALTER TABLE `refresh_tokens`
  ADD CONSTRAINT `refresh_tokens_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `reviews`
--
ALTER TABLE `reviews`
  ADD CONSTRAINT `fk_reviews_booking` FOREIGN KEY (`booking_id`) REFERENCES `bookings` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `fk_reviews_creator` FOREIGN KEY (`creator_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `fk_reviews_reviewer` FOREIGN KEY (`reviewer_id`) REFERENCES `users` (`id`) ON DELETE CASCADE;
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
