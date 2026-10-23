import { test, expect } from "@playwright/test";

const email = `e2e-${Date.now()}@example.test`;
const password = "E2E-CommuneX-Password-123!";

test("a visitor can register, sign in, view their dashboard and log out", async ({ page }) => {
  await page.goto("/register");
  await page.getByLabel(/full name/i).fill("E2E Neighbour");
  await page.getByLabel(/email address/i).fill(email);
  await page.getByLabel(/^password$/i).fill(password);
  await page.getByRole("button", { name: /create account/i }).click();
  await expect(page).toHaveURL(/\/login\?registered=1/);

  await page.getByLabel(/email address/i).fill(email);
  await page.getByLabel(/password/i).fill(password);
  await page.getByRole("button", { name: /sign in/i }).click();
  await expect(page).toHaveURL(/\/dashboard/);
  await expect(page.getByText(/good to see you, e2e/i)).toBeVisible();
  await page.getByRole("button", { name: /sign out/i }).click();
  await expect(page).toHaveURL(/\/login/);
});
