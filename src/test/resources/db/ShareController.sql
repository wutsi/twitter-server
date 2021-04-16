INSERT INTO T_SECRET(id, user_id, site_id, twitter_id, access_token, access_token_secret, creation_date_time, modification_date_time)
    VALUES
        (1, 1, 1, 11, 'access-token', 'access-token-secret', now(), now()),
        (2, 2, 1, 22, 'access-token', 'access-token-secret', now(), now()),
        (666, 666, 1, 6, 'access-token-666', 'access-token-secret-666', now(), now());
