--
-- PostgreSQL database dump
--

-- Dumped from database version 14.2
-- Dumped by pg_dump version 14.2

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: status_order; Type: TYPE; Schema: public; Owner: postgres
--

CREATE TYPE public.status_order AS ENUM (
    'Cart',
    'Accepted',
    'Cooking',
    'Ready',
    'Completed'
);


ALTER TYPE public.status_order OWNER TO postgres;

--
-- Name: type_order; Type: TYPE; Schema: public; Owner: postgres
--

CREATE TYPE public.type_order AS ENUM (
    'Restaurant',
    'Delivery',
    'Pickup'
);


ALTER TYPE public.type_order OWNER TO postgres;

--
-- Name: calc_order(); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.calc_order() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
    IF (tg_op = 'DELETE') THEN


        IF (SELECT recalc_order(old.order_id) IS NULL) THEN
            UPDATE "order"
            SET order_price = 0.0
            WHERE order_id = old.order_id;
            RETURN old;
        ELSE
            UPDATE "order"
            SET order_price = 0.0 + (SELECT recalc_order(old.order_id))
            WHERE order_id = old.order_id;
            RETURN old;
        END IF;

    ELSIF (tg_op = 'INSERT') THEN
        UPDATE "order"
        SET order_price = 0.0 + (SELECT recalc_order(new.order_id))
        WHERE order_id = new.order_id;
        RETURN new;
    ELSIF (tg_op = 'UPDATE') THEN
        UPDATE "order"
        SET order_price = 0.0 + (SELECT recalc_order(new.order_id))
        WHERE order_id = new.order_id;
        RETURN new;
    END IF;
END;
$$;


ALTER FUNCTION public.calc_order() OWNER TO postgres;

--
-- Name: calc_pizza(); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.calc_pizza() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
    IF (tg_op = 'DELETE') THEN
        UPDATE pizza
        SET pizza_price = 0.0 + (SELECT recalc_pizza(old.pizza_id))
        WHERE pizza_id = old.pizza_id;
        RETURN old;
    ELSIF (tg_op = 'INSERT') THEN
        UPDATE pizza
        SET pizza_price = 0.0 + (SELECT recalc_pizza(new.pizza_id))
        WHERE pizza_id = new.pizza_id;
        RETURN new;
    END IF;
END;
$$;


ALTER FUNCTION public.calc_pizza() OWNER TO postgres;

--
-- Name: recalc_order(integer); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.recalc_order(target_id integer) RETURNS numeric
    LANGUAGE plpgsql
    AS $$
BEGIN
    RETURN (0.0 + (
        SELECT SUM(p.pizza_price * pio.amount)
        FROM "order" o
                 JOIN public.pizza_in_order pio ON o.order_id = pio.order_id
                 JOIN pizza p ON pio.pizza_id = p.pizza_id
        WHERE o.order_id = target_id)
        );
END;

$$;


ALTER FUNCTION public.recalc_order(target_id integer) OWNER TO postgres;

--
-- Name: recalc_pizza(integer); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.recalc_pizza(target_id integer) RETURNS numeric
    LANGUAGE plpgsql
    AS $$
BEGIN
    RETURN (SELECT (
                       SELECT SUM(ingredient_price)
                       FROM (SELECT pizza_id, ingredient_price
                             FROM ingredients_to_pizza
                                      JOIN ingredient i ON i.ingredient_id = ingredients_to_pizza.ingredient_id) AS tab
                       WHERE pizza_id = target_id
                   ) * (
                       SELECT size_mod
                       FROM size_mods
                       WHERE size_id = (SELECT size_id FROM pizza WHERE pizza_id = target_id)
                   ));
END;

$$;


ALTER FUNCTION public.recalc_pizza(target_id integer) OWNER TO postgres;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: delivery; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.delivery (
    order_id integer NOT NULL,
    employee_id integer NOT NULL,
    delivery_address character varying(100) NOT NULL
);


ALTER TABLE public.delivery OWNER TO postgres;

--
-- Name: employee; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.employee (
    employee_id integer NOT NULL,
    restaurant_id integer NOT NULL,
    employee_name character varying(50) NOT NULL,
    post_id integer NOT NULL,
    employee_surname character varying(50) NOT NULL,
    employee_fathers_name character varying(50) NOT NULL
);


ALTER TABLE public.employee OWNER TO postgres;

--
-- Name: employee_employee_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.employee_employee_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.employee_employee_id_seq OWNER TO postgres;

--
-- Name: employee_employee_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.employee_employee_id_seq OWNED BY public.employee.employee_id;


--
-- Name: ingredient; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.ingredient (
    ingredient_id integer NOT NULL,
    ingredient_name character varying(50) NOT NULL,
    ingredient_price numeric(10,2) NOT NULL,
    CONSTRAINT ingredient_ingredient_price_check CHECK ((ingredient_price >= (0)::numeric))
);


ALTER TABLE public.ingredient OWNER TO postgres;

--
-- Name: ingredient_ingredient_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.ingredient_ingredient_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.ingredient_ingredient_id_seq OWNER TO postgres;

--
-- Name: ingredient_ingredient_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.ingredient_ingredient_id_seq OWNED BY public.ingredient.ingredient_id;


--
-- Name: ingredients_to_pizza; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.ingredients_to_pizza (
    ingredient_id integer NOT NULL,
    pizza_id integer NOT NULL
);


ALTER TABLE public.ingredients_to_pizza OWNER TO postgres;

--
-- Name: order; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public."order" (
    order_id integer NOT NULL,
    order_price numeric(10,2) DEFAULT 0.0 NOT NULL,
    restaurant_id integer NOT NULL,
    employee_id integer NOT NULL,
    order_date timestamp with time zone NOT NULL,
    order_type public.type_order NOT NULL,
    order_status public.status_order NOT NULL
);


ALTER TABLE public."order" OWNER TO postgres;

--
-- Name: order_order_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.order_order_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.order_order_id_seq OWNER TO postgres;

--
-- Name: order_order_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.order_order_id_seq OWNED BY public."order".order_id;


--
-- Name: pizza; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.pizza (
    pizza_id integer NOT NULL,
    pizza_name character varying(50) NOT NULL,
    size_id integer NOT NULL,
    pizza_price numeric(10,2) DEFAULT 0.0 NOT NULL,
    CONSTRAINT pizza_pizza_price_check CHECK ((pizza_price >= (0)::numeric))
);


ALTER TABLE public.pizza OWNER TO postgres;

--
-- Name: pizza_in_order; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.pizza_in_order (
    pizza_id integer NOT NULL,
    order_id integer NOT NULL,
    amount integer NOT NULL,
    employee_id integer NOT NULL
);


ALTER TABLE public.pizza_in_order OWNER TO postgres;

--
-- Name: pizza_pizza_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.pizza_pizza_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.pizza_pizza_id_seq OWNER TO postgres;

--
-- Name: pizza_pizza_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.pizza_pizza_id_seq OWNED BY public.pizza.pizza_id;


--
-- Name: post; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.post (
    post_id integer NOT NULL,
    post_name character varying(50) NOT NULL,
    post_cook boolean NOT NULL,
    post_cashbox boolean NOT NULL,
    post_delivery boolean NOT NULL
);


ALTER TABLE public.post OWNER TO postgres;

--
-- Name: post_post_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.post_post_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.post_post_id_seq OWNER TO postgres;

--
-- Name: post_post_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.post_post_id_seq OWNED BY public.post.post_id;


--
-- Name: restaurant; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.restaurant (
    restaurant_id integer NOT NULL,
    restaurant_address character varying(100) NOT NULL
);


ALTER TABLE public.restaurant OWNER TO postgres;

--
-- Name: restaurant_restaurant_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.restaurant_restaurant_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.restaurant_restaurant_id_seq OWNER TO postgres;

--
-- Name: restaurant_restaurant_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.restaurant_restaurant_id_seq OWNED BY public.restaurant.restaurant_id;


--
-- Name: review; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.review (
    pizza_id integer NOT NULL,
    order_id integer NOT NULL,
    review_rate integer NOT NULL,
    review_text character varying(2000),
    CONSTRAINT review_review_rate_check CHECK (((review_rate >= 0) AND (review_rate <= 5)))
);


ALTER TABLE public.review OWNER TO postgres;

--
-- Name: size_mods; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.size_mods (
    size_id integer NOT NULL,
    size_cm integer NOT NULL,
    size_mod numeric(4,2) NOT NULL
);


ALTER TABLE public.size_mods OWNER TO postgres;

--
-- Name: size_mods_size_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.size_mods_size_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.size_mods_size_id_seq OWNER TO postgres;

--
-- Name: size_mods_size_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.size_mods_size_id_seq OWNED BY public.size_mods.size_id;


--
-- Name: employee employee_id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.employee ALTER COLUMN employee_id SET DEFAULT nextval('public.employee_employee_id_seq'::regclass);


--
-- Name: ingredient ingredient_id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.ingredient ALTER COLUMN ingredient_id SET DEFAULT nextval('public.ingredient_ingredient_id_seq'::regclass);


--
-- Name: order order_id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public."order" ALTER COLUMN order_id SET DEFAULT nextval('public.order_order_id_seq'::regclass);


--
-- Name: pizza pizza_id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.pizza ALTER COLUMN pizza_id SET DEFAULT nextval('public.pizza_pizza_id_seq'::regclass);


--
-- Name: post post_id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.post ALTER COLUMN post_id SET DEFAULT nextval('public.post_post_id_seq'::regclass);


--
-- Name: restaurant restaurant_id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.restaurant ALTER COLUMN restaurant_id SET DEFAULT nextval('public.restaurant_restaurant_id_seq'::regclass);


--
-- Name: size_mods size_id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.size_mods ALTER COLUMN size_id SET DEFAULT nextval('public.size_mods_size_id_seq'::regclass);


--
-- Data for Name: delivery; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.delivery (order_id, employee_id, delivery_address) FROM stdin;
2	5	89731 Irving Corner
4	4	067 Haley Junction
\.


--
-- Data for Name: employee; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.employee (employee_id, restaurant_id, employee_name, post_id, employee_surname, employee_fathers_name) FROM stdin;
1	1	Mickey	1	Willms	Valentin
2	2	Troy	1	Powlowski	Isabelle
3	1	Roy	1	Little	Austin
4	2	Arlyne	1	O'Keefe	Margret
5	2	Tressie	1	Harris	Elroy
6	1	Kenya	1	Morissette	Johnathan
\.


--
-- Data for Name: ingredient; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.ingredient (ingredient_id, ingredient_name, ingredient_price) FROM stdin;
1	Bear	77.74
2	Fruit mixture	58.89
3	Black beans and brown rice	43.00
4	Rice cake	217.15
5	Macaroni or noodles with cheese and tuna	179.40
\.


--
-- Data for Name: ingredients_to_pizza; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.ingredients_to_pizza (ingredient_id, pizza_id) FROM stdin;
2	1
1	1
4	1
5	1
2	2
1	2
4	2
5	2
4	3
2	3
5	3
4	4
2	4
5	4
5	5
3	5
1	5
4	5
2	5
5	6
3	6
1	6
4	6
2	6
4	7
1	7
3	7
2	7
5	7
4	8
1	8
3	8
2	8
5	8
4	9
3	9
5	9
1	9
2	9
4	10
3	10
5	10
1	10
2	10
\.


--
-- Data for Name: order; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public."order" (order_id, order_price, restaurant_id, employee_id, order_date, order_type, order_status) FROM stdin;
1	1382.84	1	1	2021-09-24 20:02:33+03	Pickup	Completed
2	2535.20	2	5	2022-04-25 12:13:21+03	Delivery	Accepted
3	2823.80	1	6	2022-04-25 14:10:39+03	Pickup	Cart
4	2714.08	2	4	2022-04-25 12:13:47+03	Delivery	Ready
5	455.44	1	6	2022-04-25 14:06:35+03	Restaurant	Accepted
6	6154.64	1	1	2022-04-25 09:46:22+03	Pickup	Cart
7	3209.01	1	6	2022-04-25 11:14:43+03	Restaurant	Ready
8	5429.04	2	5	2022-04-25 13:03:10+03	Restaurant	Completed
9	3699.60	1	6	2022-04-25 12:54:26+03	Pickup	Accepted
10	2111.33	2	2	2022-04-25 10:46:17+03	Restaurant	Completed
\.


--
-- Data for Name: pizza; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.pizza (pizza_id, pizza_name, size_id, pizza_price) FROM stdin;
1	Fruit MixtuBearRice CakeMacaroni Or	1	533.18
2	Fruit MixtuBearRice CakeMacaroni Or	2	639.82
3	Rice CakeFruit MixtureMacaroni Or Noo	1	455.44
4	Rice CakeFruit MixtureMacaroni Or Noo	2	546.53
5	Macaroni Black BeaBearRice CakeFruit Mix	1	576.18
6	Macaroni Black BeaBearRice CakeFruit Mix	2	691.42
7	Rice CakeBearBlack BeaFruit MixMacaroni 	1	576.18
8	Rice CakeBearBlack BeaFruit MixMacaroni 	2	691.42
9	Rice CakeBlack BeaMacaroni BearFruit Mix	1	576.18
10	Rice CakeBlack BeaMacaroni BearFruit Mix	2	691.42
\.


--
-- Data for Name: pizza_in_order; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.pizza_in_order (pizza_id, order_id, amount, employee_id) FROM stdin;
8	1	2	6
5	2	2	2
6	2	2	2
3	3	1	6
2	3	1	6
9	3	3	6
8	4	3	2
2	4	1	2
3	5	1	6
9	6	3	6
7	6	3	6
2	6	1	6
3	6	3	6
6	6	1	6
4	7	1	1
2	7	2	1
8	7	2	1
1	8	1	5
5	8	3	5
10	8	3	5
4	8	2	5
2	9	2	6
5	9	3	6
8	9	1	6
3	10	1	4
1	10	1	4
4	10	1	4
5	10	1	4
\.


--
-- Data for Name: post; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.post (post_id, post_name, post_cook, post_cashbox, post_delivery) FROM stdin;
1	Universal Soldier	t	t	t
\.


--
-- Data for Name: restaurant; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.restaurant (restaurant_id, restaurant_address) FROM stdin;
1	695 Eusebia Harbors
2	09281 Hessel Junctions
\.


--
-- Data for Name: review; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.review (pizza_id, order_id, review_rate, review_text) FROM stdin;
\.


--
-- Data for Name: size_mods; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.size_mods (size_id, size_cm, size_mod) FROM stdin;
1	23	1.00
2	25	1.20
\.


--
-- Name: employee_employee_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.employee_employee_id_seq', 6, true);


--
-- Name: ingredient_ingredient_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.ingredient_ingredient_id_seq', 5, true);


--
-- Name: order_order_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.order_order_id_seq', 10, true);


--
-- Name: pizza_pizza_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.pizza_pizza_id_seq', 10, true);


--
-- Name: post_post_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.post_post_id_seq', 1, true);


--
-- Name: restaurant_restaurant_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.restaurant_restaurant_id_seq', 2, true);


--
-- Name: size_mods_size_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.size_mods_size_id_seq', 2, true);


--
-- Name: delivery delivery_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.delivery
    ADD CONSTRAINT delivery_pkey PRIMARY KEY (order_id);


--
-- Name: employee employee_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.employee
    ADD CONSTRAINT employee_pkey PRIMARY KEY (employee_id);


--
-- Name: ingredient ingredient_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.ingredient
    ADD CONSTRAINT ingredient_pkey PRIMARY KEY (ingredient_id);


--
-- Name: ingredients_to_pizza ingredients_to_pizza_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.ingredients_to_pizza
    ADD CONSTRAINT ingredients_to_pizza_pkey PRIMARY KEY (ingredient_id, pizza_id);


--
-- Name: order order_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public."order"
    ADD CONSTRAINT order_pkey PRIMARY KEY (order_id);


--
-- Name: pizza_in_order pizza_in_order_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.pizza_in_order
    ADD CONSTRAINT pizza_in_order_pkey PRIMARY KEY (pizza_id, order_id);


--
-- Name: pizza pizza_pizza_name_size_id_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.pizza
    ADD CONSTRAINT pizza_pizza_name_size_id_key UNIQUE (pizza_name, size_id);


--
-- Name: pizza pizza_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.pizza
    ADD CONSTRAINT pizza_pkey PRIMARY KEY (pizza_id);


--
-- Name: post post_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.post
    ADD CONSTRAINT post_pkey PRIMARY KEY (post_id);


--
-- Name: post post_post_name_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.post
    ADD CONSTRAINT post_post_name_key UNIQUE (post_name);


--
-- Name: restaurant restaurant_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.restaurant
    ADD CONSTRAINT restaurant_pkey PRIMARY KEY (restaurant_id);


--
-- Name: restaurant restaurant_restaurant_address_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.restaurant
    ADD CONSTRAINT restaurant_restaurant_address_key UNIQUE (restaurant_address);


--
-- Name: review review_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.review
    ADD CONSTRAINT review_pkey PRIMARY KEY (pizza_id, order_id);


--
-- Name: size_mods size_mods_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.size_mods
    ADD CONSTRAINT size_mods_pkey PRIMARY KEY (size_id);


--
-- Name: size_mods size_mods_size_cm_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.size_mods
    ADD CONSTRAINT size_mods_size_cm_key UNIQUE (size_cm);


--
-- Name: pizza_in_order order_price_trigger_delete; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER order_price_trigger_delete AFTER DELETE ON public.pizza_in_order FOR EACH ROW EXECUTE FUNCTION public.calc_order();


--
-- Name: pizza_in_order order_price_trigger_insert; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER order_price_trigger_insert AFTER INSERT ON public.pizza_in_order FOR EACH ROW EXECUTE FUNCTION public.calc_order();


--
-- Name: pizza_in_order order_price_trigger_update; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER order_price_trigger_update AFTER UPDATE ON public.pizza_in_order FOR EACH ROW EXECUTE FUNCTION public.calc_order();


--
-- Name: ingredients_to_pizza pizza_price_trigger; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER pizza_price_trigger AFTER INSERT OR DELETE ON public.ingredients_to_pizza FOR EACH ROW EXECUTE FUNCTION public.calc_pizza();


--
-- Name: delivery delivery_employee_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.delivery
    ADD CONSTRAINT delivery_employee_id_fkey FOREIGN KEY (employee_id) REFERENCES public.employee(employee_id) ON DELETE CASCADE;


--
-- Name: delivery delivery_order_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.delivery
    ADD CONSTRAINT delivery_order_id_fkey FOREIGN KEY (order_id) REFERENCES public."order"(order_id) ON DELETE CASCADE;


--
-- Name: employee employee_post_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.employee
    ADD CONSTRAINT employee_post_id_fkey FOREIGN KEY (post_id) REFERENCES public.post(post_id);


--
-- Name: employee employee_restaurant_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.employee
    ADD CONSTRAINT employee_restaurant_id_fkey FOREIGN KEY (restaurant_id) REFERENCES public.restaurant(restaurant_id);


--
-- Name: ingredients_to_pizza ingredients_to_pizza_ingredient_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.ingredients_to_pizza
    ADD CONSTRAINT ingredients_to_pizza_ingredient_id_fkey FOREIGN KEY (ingredient_id) REFERENCES public.ingredient(ingredient_id) ON DELETE RESTRICT;


--
-- Name: ingredients_to_pizza ingredients_to_pizza_pizza_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.ingredients_to_pizza
    ADD CONSTRAINT ingredients_to_pizza_pizza_id_fkey FOREIGN KEY (pizza_id) REFERENCES public.pizza(pizza_id) ON DELETE CASCADE;


--
-- Name: order order_employee_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public."order"
    ADD CONSTRAINT order_employee_id_fkey FOREIGN KEY (employee_id) REFERENCES public.employee(employee_id) ON DELETE CASCADE;


--
-- Name: order order_restaurant_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public."order"
    ADD CONSTRAINT order_restaurant_id_fkey FOREIGN KEY (restaurant_id) REFERENCES public.restaurant(restaurant_id) ON DELETE CASCADE;


--
-- Name: pizza_in_order pizza_in_order_employee_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.pizza_in_order
    ADD CONSTRAINT pizza_in_order_employee_id_fkey FOREIGN KEY (employee_id) REFERENCES public.employee(employee_id) ON DELETE RESTRICT;


--
-- Name: pizza_in_order pizza_in_order_order_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.pizza_in_order
    ADD CONSTRAINT pizza_in_order_order_id_fkey FOREIGN KEY (order_id) REFERENCES public."order"(order_id) ON DELETE CASCADE;


--
-- Name: pizza_in_order pizza_in_order_pizza_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.pizza_in_order
    ADD CONSTRAINT pizza_in_order_pizza_id_fkey FOREIGN KEY (pizza_id) REFERENCES public.pizza(pizza_id) ON DELETE RESTRICT;


--
-- Name: pizza pizza_size_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.pizza
    ADD CONSTRAINT pizza_size_id_fkey FOREIGN KEY (size_id) REFERENCES public.size_mods(size_id);


--
-- Name: review review_order_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.review
    ADD CONSTRAINT review_order_id_fkey FOREIGN KEY (order_id) REFERENCES public."order"(order_id) ON DELETE CASCADE;


--
-- Name: review review_pizza_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.review
    ADD CONSTRAINT review_pizza_id_fkey FOREIGN KEY (pizza_id) REFERENCES public.pizza(pizza_id) ON DELETE CASCADE;


--
-- PostgreSQL database dump complete
--

