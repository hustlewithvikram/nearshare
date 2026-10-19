-- Run in the Supabase SQL Editor for NearShare.
-- Safe migration for existing installs: add optional coordinates for nearby discovery.
alter table public.listings add column if not exists latitude double precision;
alter table public.listings add column if not exists longitude double precision;
alter table public.listings drop constraint if exists listings_coordinates_pair_check;
alter table public.listings add constraint listings_coordinates_pair_check check ((latitude is null and longitude is null) or (latitude between -90 and 90 and longitude between -180 and 180));
create extension if not exists pgcrypto;
create table if not exists public.listings (
 id uuid primary key default gen_random_uuid(),
 owner_id uuid not null references auth.users(id) on delete cascade,
 title text not null check (char_length(title) between 1 and 80),
 description text not null check (char_length(description) between 1 and 1000),
 category text not null,
 listing_type text not null check (listing_type in ('Borrow','Give away')),
 item_condition text not null check (item_condition in ('New','Like new','Good','Fair','For parts')),
 location_text text not null check (char_length(location_text) between 1 and 120),
 image_url text,
 status text not null default 'active' check (status in ('active','paused','completed')),
 created_at timestamptz not null default now(),
 updated_at timestamptz not null default now()
);
-- Safe migration for existing installs: add optional coordinates for nearby discovery.
alter table public.listings add column if not exists latitude double precision;
alter table public.listings add column if not exists longitude double precision;
alter table public.listings drop constraint if exists listings_coordinates_pair_check;
alter table public.listings add constraint listings_coordinates_pair_check check ((latitude is null and longitude is null) or (latitude between -90 and 90 and longitude between -180 and 180));
create table if not exists public.listing_requests (
 id uuid primary key default gen_random_uuid(),
 listing_id uuid not null references public.listings(id) on delete cascade,
 requester_id uuid not null references auth.users(id) on delete cascade,
 message text not null default '' check (char_length(message) <= 500),
 status text not null default 'pending' check (status in ('pending','accepted','declined','cancelled','completed')),
 created_at timestamptz not null default now(),
 updated_at timestamptz not null default now()
);
create index if not exists listing_requests_listing_idx on public.listing_requests(listing_id, created_at desc);
create index if not exists listing_requests_requester_idx on public.listing_requests(requester_id, created_at desc);
alter table public.listing_requests enable row level security;
drop policy if exists "Requesters and owners can view requests" on public.listing_requests;
create policy "Requesters and owners can view requests" on public.listing_requests for select to authenticated using (auth.uid() = requester_id or exists(select 1 from public.listings l where l.id = listing_id and l.owner_id = auth.uid()));
drop policy if exists "Members can request others listings" on public.listing_requests;
create policy "Members can request others listings" on public.listing_requests for insert to authenticated with check (auth.uid() = requester_id and exists(select 1 from public.listings l where l.id = listing_id and l.owner_id <> auth.uid() and l.status = 'active'));
drop policy if exists "Requesters cancel pending requests" on public.listing_requests;
create policy "Requesters cancel pending requests" on public.listing_requests for update to authenticated using (auth.uid() = requester_id and status = 'pending') with check (auth.uid() = requester_id and status = 'cancelled');
drop policy if exists "Owners manage requests" on public.listing_requests;
create policy "Owners manage requests" on public.listing_requests for update to authenticated using (exists(select 1 from public.listings l where l.id = listing_id and l.owner_id = auth.uid())) with check (exists(select 1 from public.listings l where l.id = listing_id and l.owner_id = auth.uid()));

create index if not exists listings_active_created_at_idx on public.listings (created_at desc) where status='active';
create index if not exists listings_owner_id_idx on public.listings (owner_id);
alter table public.listings enable row level security;
drop policy if exists "Anyone can read active listings" on public.listings;
create policy "Anyone can read active listings" on public.listings for select using (status='active' or auth.uid()=owner_id);
drop policy if exists "Members can create their own listings" on public.listings;
create policy "Members can create their own listings" on public.listings for insert to authenticated with check (auth.uid()=owner_id);
drop policy if exists "Members can update their own listings" on public.listings;
create policy "Members can update their own listings" on public.listings for update to authenticated using (auth.uid()=owner_id) with check (auth.uid()=owner_id);
drop policy if exists "Members can delete their own listings" on public.listings;
create policy "Members can delete their own listings" on public.listings for delete to authenticated using (auth.uid()=owner_id);
insert into storage.buckets(id,name,public,file_size_limit,allowed_mime_types)
values('nearshare-listing-images','nearshare-listing-images',true,8388608,array['image/jpeg','image/png','image/webp'])
on conflict(id) do update set public=true,file_size_limit=8388608,allowed_mime_types=array['image/jpeg','image/png','image/webp'];
drop policy if exists "Public can view listing images" on storage.objects;
create policy "Public can view listing images" on storage.objects for select using(bucket_id='nearshare-listing-images');
drop policy if exists "Members upload images to their own folder" on storage.objects;
create policy "Members upload images to their own folder" on storage.objects for insert to authenticated with check(bucket_id='nearshare-listing-images' and (storage.foldername(name))[1]=auth.uid()::text);
drop policy if exists "Members update images in their own folder" on storage.objects;
create policy "Members update images in their own folder" on storage.objects for update to authenticated using(bucket_id='nearshare-listing-images' and (storage.foldername(name))[1]=auth.uid()::text) with check(bucket_id='nearshare-listing-images' and (storage.foldername(name))[1]=auth.uid()::text);
drop policy if exists "Members delete images in their own folder" on storage.objects;
create policy "Members delete images in their own folder" on storage.objects for delete to authenticated using(bucket_id='nearshare-listing-images' and (storage.foldername(name))[1]=auth.uid()::text);

-- Phase 6: member safety reports.
create table if not exists public.listing_reports (
 id uuid primary key default gen_random_uuid(),
 listing_id uuid not null references public.listings(id) on delete cascade,
 reporter_id uuid not null references auth.users(id) on delete cascade,
 reason text not null check (char_length(reason) between 1 and 80),
 details text not null default '' check (char_length(details) <= 1000),
 status text not null default 'open' check (status in ('open','reviewing','resolved','dismissed')),
 created_at timestamptz not null default now()
);
create index if not exists listing_reports_status_created_idx on public.listing_reports(status, created_at desc);
alter table public.listing_reports enable row level security;
drop policy if exists "Members can report other members listings" on public.listing_reports;
create policy "Members can report other members listings" on public.listing_reports for insert to authenticated with check (auth.uid() = reporter_id and exists (select 1 from public.listings l where l.id = listing_id and l.owner_id <> auth.uid()));
-- Reports are not readable by ordinary members. A trusted moderation workflow is required to review them.
